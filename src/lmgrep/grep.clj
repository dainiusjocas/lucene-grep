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
  "TODO: overlapping highlights are combined under one color, maybe solve it?"
  [line-str highlights]
  (when (seq highlights)
    (loop [[[ann next-ann] & ann-pairs] (partition 2 1 nil highlights)
           acc ""
           last-position 0]
      (let [prefix (subs line-str last-position (max last-position (:begin-offset ann)))
            highlight (let [text-to-highlight (subs line-str (:begin-offset ann) (:end-offset ann))]
                        (if (< (:begin-offset ann) last-position)
                          ; adjusting highlight text for overlap
                          (red-text (subs text-to-highlight (- last-position (:begin-offset ann))))
                          (red-text text-to-highlight)))
            suffix (if (nil? next-ann)
                     (subs line-str (:end-offset ann))
                     (subs line-str (:end-offset ann) (max (:begin-offset next-ann)
                                                           (:end-offset ann))))]
        (if (nil? next-ann)
          (str acc prefix highlight suffix)
          (recur ann-pairs
                 (str acc prefix highlight suffix)
                 (long (max (:begin-offset next-ann)
                            (:end-offset ann)))))))))

(defn string-output [highlights {:keys [file line-number line]} options]
  (if-let [template (:template options)]
    (-> template
        (str/replace "{{file}}" file)
        (str/replace "{{line-number}}" (str line-number))
        (str/replace "{{highlighted-line}}" (highlight-line line highlights))
        (str/replace "{{line}}" line))
    (format "%s:%s:%s"
           (purple-text (or file "*STDIN*"))
           (green-text line-number)
           (highlight-line line highlights))))

(defn match-lines [highlighter-fn file-path lines options]
  (doseq [[line-str line-number] (map (fn [line-str line-number] [line-str line-number])
                                      lines (range))]
    (when-let [highlights (seq (highlighter-fn line-str))]
      (let [details {:file        file-path
                     :line-number (inc line-number)
                     :line        line-str}]
        (println (case (:format options)
                  :edn (pr-str details)
                  :json (json/write-value-as-string details)
                  :string (string-output highlights details options)
                  (string-output highlights details options)))))))

(defn grep [query-string files-pattern options]
  (let [dictionary [(merge {:text            query-string
                            :case-sensitive? false
                            :ascii-fold?     true
                            :stem?           true
                            :slop            0
                            :tokenizer       :standard
                            :stemmer         :english}
                           options)]
        highlighter-fn (lucene/highlighter dictionary)]
    (if files-pattern
      (doseq [path (fs/get-files files-pattern)]
        (with-open [rdr (io/reader path)]
          (match-lines highlighter-fn path (line-seq rdr) options)))
      (when (.ready ^Reader *in*)
        (match-lines highlighter-fn nil (line-seq (BufferedReader. *in*)) options)))))

(comment
  (lmgrep.grep/grep "opt" "**.md" {:format :edn}))
