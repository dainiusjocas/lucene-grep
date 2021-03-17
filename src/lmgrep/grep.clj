(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.fs :as fs]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene :as lucene])
  (:import (java.io BufferedReader Reader)))

(defn compact [m] (into {} (remove (comp nil? second) m)))

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defn match-lines [highlighter-fn file-path lines options]
  (doseq [[line-str line-number] (map (fn [line-str line-number] [line-str line-number])
                                      lines (range))]
    (when-let [highlights (seq (highlighter-fn line-str (select-keys options [:with-score])))]
      (let [details (compact {:file        file-path
                              :line-number (inc line-number)
                              :line        line-str
                              :score       (sum-score highlights)})]
        (println (case (:format options)
                   :edn (pr-str details)
                   :json (json/write-value-as-string details)
                   :string (formatter/string-output highlights details options)
                   (formatter/string-output highlights details options)))))))

(def default-text-analysis
  {:case-sensitive?             false
   :ascii-fold?                 true
   :stem?                       true
   :tokenizer                   :standard
   :stemmer                     :english
   :word-delimiter-graph-filter 0})

(defn grep [lucene-query-strings files-pattern files options]
  (let [analysis-options (merge default-text-analysis options)
        dictionary (map (fn [lqs] (assoc analysis-options :text lqs)) lucene-query-strings)
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
