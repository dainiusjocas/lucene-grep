(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.fs :as fs]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene :as lucene]
            [lmgrep.lucene.dictionary :as dictionary]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (java.io BufferedReader)
           (org.apache.lucene.analysis Analyzer)))

(defn compact [m] (into {} (remove (comp nil? second) m)))

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defn printable-highlights [highlights]
  (map (fn [h] (-> h
                   (assoc :query (:text h))
                   (dissoc :text))) highlights))

(defn match-lines [highlighter-fn file-path lines options]
  (doseq [[line-str line-number] (map (fn [line-str line-number] [line-str line-number])
                                      lines (range))]
    (if-let [highlights (seq (highlighter-fn line-str (select-keys options [:with-score :with-scored-highlights])))]
      (let [details (compact {:file        file-path
                              :line-number (inc line-number)
                              :line        line-str
                              :score       (sum-score highlights)
                              :highlights  (if (:with-details options)
                                             (printable-highlights highlights)
                                             nil)})]
        (println (case (:format options)
                   :edn (pr-str details)
                   :json (json/write-value-as-string details)
                   :string (formatter/string-output highlights details options)
                   (formatter/string-output highlights details options))))
      (when (:with-empty-lines options)
        (println)))))

(defn grep [lucene-query-strings files-pattern files options]
  (let [dictionary (dictionary/prepare-dictionary lucene-query-strings options)
        highlighter-fn (lucene/highlighter dictionary)]
    (if files-pattern
      (doseq [path (concat (fs/get-files files-pattern options)
                           (fs/filter-files files))]
        (if (:split options)
          (with-open [rdr (io/reader path)]
            (match-lines highlighter-fn path (line-seq rdr) options))
          (match-lines highlighter-fn path [(slurp path)] options)))
      (if (:split options)
        (match-lines highlighter-fn nil (line-seq (BufferedReader. *in*)) options)
        (match-lines highlighter-fn nil [(str/trim (slurp *in*))] options)))))

(comment
  (lmgrep.grep/grep ["opt"] "**.md" nil {:format :edn})

  (lmgrep.grep/grep ["test" "opt"] "**.md" nil {:split true})

  (time (lmgrep.grep/grep ["opt"] "**.class" nil {:format            :edn
                                                  :skip-binary-files true})))

(defn analyze-text [file-path ^Analyzer analyzer]
  (with-open [^BufferedReader rdr (if file-path (io/reader file-path) (BufferedReader. *in*))]
    (loop [^String line (.readLine rdr)]
      (when line
        (println
          (json/write-value-as-string
            (text-analysis/text->token-strings line analyzer)))
        (recur (.readLine rdr))))))

(defn analyze-lines
  "Sequence of text into sequence of text token sequences. Output format is JSON.
  If given file path reads file otherwise stdin."
  [files-pattern files options]
  (let [analyzer (text-analysis/analyzer-constructor options)]
    (if files-pattern
      (doseq [path (concat (fs/get-files files-pattern options)
                           (fs/filter-files files))]
        (analyze-text path analyzer))
      (analyze-text nil analyzer))))

(comment
  (lmgrep.grep/analyze-lines "test/resources/test.txt" {}))
