(ns core
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [beagle.phrases :as phrases])
  (:gen-class)
  (:import (java.nio.file FileSystems PathMatcher Path)
           (java.io File BufferedReader Reader)))

(defn get-files [^String glob]
  (let [glob-file (io/file glob)
        starting-folder (or (.getParent glob-file) ".")
        filename-glob (.getName glob-file)
        ^PathMatcher grammar-matcher (.getPathMatcher
                                       (FileSystems/getDefault)
                                       (str "glob:" filename-glob))]
    (->> starting-folder
         io/file
         file-seq
         (filter #(.isFile ^File %))
         (filter #(.matches grammar-matcher (.getFileName ^Path (.toPath ^File %))))
         (mapv #(.getPath ^File %)))))

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
            highlight (if (< (:begin-offset ann) last-position)
                        ; adjusting highlight text for overlap
                        (red-text (subs (:text ann) (- last-position (:begin-offset ann))))
                        (red-text (:text ann)))
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

(defn match-lines [highlighter-fn file-path lines]
  (doseq [[line-str line-number] (map (fn [line-str line-number] [line-str line-number])
                                      lines (range))]
    (when-let [highlights (seq (highlighter-fn line-str))]
      (println
        (format "%s:%s:%s"
                (purple-text (or file-path "*STDIN*"))
                (green-text (inc line-number))
                (highlight-line line-str highlights))))))

(defn match [query-string files-pattern options]
  (let [dictionary [(merge {:text            query-string
                            :case-sensitive? false
                            :ascii-fold?     true
                            :stem?           true
                            :slop            0
                            :tokenizer       :standard
                            :stemmer         :english}
                           options)]
        highlighter-fn (phrases/highlighter dictionary)]
    (if files-pattern
      (doseq [path (get-files files-pattern)]
        (with-open [rdr (io/reader path)]
          (match-lines highlighter-fn path (line-seq rdr))))
      (when (.ready ^Reader *in*)
        (match-lines highlighter-fn nil (line-seq (BufferedReader. *in*)))))))

; TODO: list supported tokenizers, stemmers,
(def cli-options
  [[nil "--tokenizer TOKENIZER" "Tokenizer to use"
    :parse-fn #(keyword %)]
   [nil "--case-sensitive? CASE_SENSITIVE" "If text should be case sensitive"
    :parse-fn #(Boolean/parseBoolean %)
    :default false]
   [nil "--ascii-fold? ASCII_FOLDED" "If text should be ascii folded"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   [nil "--stem? STEMMED" "If text should be stemmed"
    :parse-fn #(Boolean/parseBoolean %)
    :default true]
   [nil "--stemmer STEMMER" "Which stemmer to use for stemming"
    :parse-fn #(keyword %)]
   [nil "--slop SLOP" "How far can be words from each other"
    :parse-fn #(Integer/parseInt %)
    :default 0]
   [nil "--in-order? IN_ORDER" "Should the phrase be ordered in matches with a non-zero slop"
    :parse-fn #(Boolean/parseBoolean %)
    :default false]
   ["-h" "--help"]])

(comment
  (cli/parse-opts ["--slop=2" "--tokenizer=standard" "--stem?=false" "--stemmer=english" "--case-sensitive?=true"] cli-options)
  (cli/parse-opts ["test"] cli-options))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (when (seq errors)
      (println "Errors:" errors)
      (System/exit 1))
    (when (or (:help options) (zero? (count arguments)))
      (println "Lucene Monitor based grep-like utility.")
      (println "Usage: lmgrep [OPTIONS] PHRASE [FILES]")
      (println "Supported options:")
      (println summary)
      (System/exit 1))
    (if (= 2 (count arguments))
      (match (nth arguments 0) (nth arguments 1) options)
      (match (nth arguments 0) nil options)))
  (System/exit 0))
