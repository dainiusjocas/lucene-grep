(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.fs :as fs]
            [lmgrep.lucene :as lucene])
  (:import (java.io BufferedReader Reader)))

(defn red-text [text]
  (str \ "[1;31m" text \ "[0m"))

(defn purple-text [text]
  (str \ "[0;35m" text \ "[0m"))

(defn green-text [text]
  (str \ "[0;32m" text \ "[0m"))

(defn highlight-line
  "TODO: overlapping phrase highlights are combined under one color, maybe solve it?"
  [line-str highlights options]
  (if (:with-score options)
    line-str
    (when (seq highlights)
      (let [highlight-fn (if (and (string? (:pre-tags options)) (string? (:post-tags options)))
                           #(str (:pre-tags options) % (:post-tags options))
                           red-text)]
        (loop [[[ann next-ann] & ann-pairs] (partition 2 1 nil highlights)
               acc ""
               last-position 0]
          (let [prefix (subs line-str last-position (max last-position (:begin-offset ann)))
                highlight (let [text-to-highlight (subs line-str (:begin-offset ann) (:end-offset ann))]
                            (if (< (:begin-offset ann) last-position)
                              ; adjusting highlight text for overlap
                              (highlight-fn (subs text-to-highlight (- last-position (:begin-offset ann))))
                              (highlight-fn text-to-highlight)))
                suffix (if (nil? next-ann)
                         (subs line-str (:end-offset ann))
                         (subs line-str (:end-offset ann) (max (:begin-offset next-ann)
                                                               (:end-offset ann))))]
            (if (nil? next-ann)
              (str acc prefix highlight suffix)
              (recur ann-pairs
                     (str acc prefix highlight suffix)
                     (long (max (:begin-offset next-ann)
                                (:end-offset ann)))))))))))

(defn string-output [highlights {:keys [file line-number line score]} options]
  (if-let [template (:template options)]
    (-> template
        (str/replace "{{file}}" (or file ""))
        (str/replace "{{line-number}}" (str line-number))
        (str/replace "{{highlighted-line}}" (highlight-line line highlights options))
        (str/replace "{{line}}" line)
        (str/replace "{{score}}" (str score)))
    (if score
      (format "%s:%s:%s:%s"
              (purple-text (or file "*STDIN*"))
              (green-text line-number)
              (purple-text score)
              (highlight-line line highlights options))
      (format "%s:%s:%s"
              (purple-text (or file "*STDIN*"))
              (green-text line-number)
              (highlight-line line highlights options)))))

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
                   :string (string-output highlights details options)
                   (string-output highlights details options)))))))

(def default-text-analysis
  {:case-sensitive?             false
   :ascii-fold?                 true
   :stem?                       true
   :tokenizer                   :standard
   :stemmer                     :english
   :word-delimiter-graph-filter 0})

(defn grep [query-string files-pattern files options]
  (let [dictionary [(merge default-text-analysis (assoc options :text query-string))]
        highlighter-fn (lucene/highlighter dictionary)]
    (if files-pattern
      (doseq [path (concat (fs/get-files files-pattern options)
                           (fs/filter-files files))]
        (if (:split options)
          (with-open [rdr (io/reader path)]
            (match-lines highlighter-fn path (line-seq rdr) options))
          (match-lines highlighter-fn path [(slurp path)] options)))
      (when (.ready ^Reader *in*)
        (if (:split options)
          (match-lines highlighter-fn nil (line-seq (BufferedReader. *in*)) options)
          (match-lines highlighter-fn nil [(str/trim (slurp *in*))] options))))))

(comment
  (lmgrep.grep/grep "opt" "**.md" nil {:format :edn})

  (time (lmgrep.grep/grep "opt" "**.class" nil {:format            :edn
                                            :skip-binary-files true})))
