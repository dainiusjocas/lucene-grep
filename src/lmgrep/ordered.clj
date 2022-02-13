(ns lmgrep.ordered
  (:require [clojure.string :as str]
            [lmgrep.matching :as matching]
            [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as impl])
  (:import (java.io BufferedReader FileReader BufferedWriter PrintWriter)
           (lmgrep.matching LineNrStr)))

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

(defn grep [files-pattern file-paths-to-analyze highlighter-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)]
    (if files-pattern
      (doseq [^String path file-paths-to-analyze]
        (if (get options :split)
          (with-open [^BufferedReader rdr (BufferedReader. (FileReader. path) reader-buffer-size)]
            (match-lines highlighter-fn path (line-seq rdr) options))
          (match-lines highlighter-fn path [(slurp path)] options)))
      (if (get options :split)
        (match-lines highlighter-fn nil (line-seq (BufferedReader. *in* reader-buffer-size)) options)
        (match-lines highlighter-fn nil [(str/trim (slurp *in*))] options)))))
