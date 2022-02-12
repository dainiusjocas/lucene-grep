(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as impl]
            [jsonista.core :as json]
            [lmgrep.fs :as fs]
            [lmgrep.matching :as matching]
            [lmgrep.lucene :as lucene]
            [lmgrep.analysis :as analysis]
            [lmgrep.unordered :as unordered])
  (:import (java.io BufferedReader File PrintWriter BufferedWriter FileReader)
           (lmgrep.matching LineNrStr)))

(set! *warn-on-reflection* true)

(defn seq-of-chan
  "Creates a lazy seq from a core.async channel."
  [c]
  (lazy-seq
    (let [fst (a/<!! c)]
      (if (nil? fst) nil (cons fst (seq-of-chan c))))))

(defn map-pipeline
  "Parallel map for compute intensive functions, backed by clojure.core.async/pipeline."
  ([f p coll]
   (let [ic (a/chan p)
         oc (a/chan p)]
     (a/onto-chan! ic coll)
     (a/pipeline (min p impl/MAX-QUEUE-SIZE)
                 oc
                 (map f)
                 ic
                 true
                 (fn [^Throwable t]
                   (.println System/err (format "Failed with: '%s'" (.toString t)))
                   (a/close! oc)
                   (System/exit 1)))
     (seq-of-chan oc)))
  ([f coll] (map-pipeline f 16 coll)))

(defn match-lines [highlighter-fn file-path lines options]
  (let [parallel-matcher (matching/matcher-fn highlighter-fn file-path options)
        concurrency (get options :concurrency 8)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        numbered-lines (map-indexed (fn [line-str line-number] (LineNrStr. line-str line-number)) lines)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))
        with-empty-lines (:with-empty-lines options)]
    (doseq [^String to-print (map-pipeline parallel-matcher concurrency numbered-lines)]
      (if (.equals "" to-print)
        (when with-empty-lines (.println writer))
        (.println writer to-print)))
    (.flush writer)))

(defn read-questionnaire-from-file [^String file-path]
  (let [^File input-file (io/file file-path)]
    (if (.isFile input-file)
      (with-open [is (io/input-stream input-file)]
        (json/read-value is json/keyword-keys-object-mapper))
      (throw (Exception. (format "File '%s' doesn't exist." file-path))))))

(def DEFAULT_QUERY_PARSER :classic)
(def DEFAULT_QUERY_PARSER_CONF {:allow-leading-wildcard true})

(defn combine-questionnaire [lucene-query-strings options]
  (into (mapv (fn [lucene-query-string] {:query             lucene-query-string
                                         :query-parser      (get options :query-parser DEFAULT_QUERY_PARSER)
                                         :query-parser-conf (merge DEFAULT_QUERY_PARSER_CONF
                                                                   (get options :query-parser-conf))})
              lucene-query-strings)
        (when-let [queries-file-path (:queries-file options)]
          (read-questionnaire-from-file queries-file-path))))

(defn grep [lucene-query-strings files-pattern files options]
  (let [questionnaire (combine-questionnaire lucene-query-strings options)
        preserve-order? (get options :preserve-order true)
        reader-buffer-size (get options :reader-buffer-size 8192)
        custom-analyzers (analysis/prepare-analyzers (get options :analyzers-file) options)
        highlighter-fn (lucene/highlighter questionnaire options custom-analyzers)
        file-paths-to-analyze (into (fs/get-files files-pattern options)
                                    (fs/filter-files files))]
    (if preserve-order?
      (if files-pattern
        (doseq [^String path file-paths-to-analyze]
          (if (:split options)
            (with-open [^BufferedReader rdr (BufferedReader. (FileReader. path) reader-buffer-size)]
              (match-lines highlighter-fn path (line-seq rdr) options))
            (match-lines highlighter-fn path [(slurp path)] options)))
        (if (:split options)
          (match-lines highlighter-fn nil (line-seq (BufferedReader. *in* reader-buffer-size)) options)
          (match-lines highlighter-fn nil [(str/trim (slurp *in*))] options)))
      (unordered/grep file-paths-to-analyze
                      highlighter-fn
                      options))))

(comment
  (lmgrep.grep/grep ["opt"] "**.md" nil {:format :edn})

  (lmgrep.grep/grep ["test" "opt"] "**.md" nil {:split true})

  (time (lmgrep.grep/grep ["opt"] "**.class" nil {:format            :edn
                                                  :skip-binary-files true})))
