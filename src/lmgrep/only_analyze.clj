(ns lmgrep.only-analyze
  (:require [jsonista.core :as json]
            [lmgrep.analysis :as analysis]
            [lmgrep.concurrent :as c]
            [lmgrep.fs :as fs]
            [lmgrep.lucene.analyzer :as analyzer]
            [lucene.custom.text-analysis :as text-analysis])
  (:import (java.io BufferedReader PrintWriter BufferedWriter FileReader)
           (org.apache.lucene.analysis Analyzer)
           (java.util.concurrent ExecutorService)))

(set! *warn-on-reflection* true)

(defn analyze-to-graph [input-reader ^PrintWriter writer analyzer]
  (with-open [^BufferedReader rdr input-reader]
    (loop [^String line (.readLine rdr)]
      (if (nil? line)
        (.flush writer)
        (do
          (.println writer (text-analysis/text->graph line analyzer))
          (recur (.readLine rdr)))))))

(defn graph [files-to-analyze writer analyzer options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)]
    (doseq [^String path files-to-analyze]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))]
        (analyze-to-graph reader writer analyzer)))))

(defn unordered-analysis
  "Reads strings from the reader line by line, processes each line on an ExecutorService
   thread pool then sends the lines to another single thread ExecutorService for writing
   the output lines to a writer.
   When the input is consumed the text analysis thread pool is gracefully shut down.
   Then the writer thread pool is gracefully shut down."
  [reader ^PrintWriter writer analysis-fn analyzer
   ^ExecutorService analyzer-thread-pool-executor
   ^ExecutorService writer-thread-pool-executor]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)]
      (when-not (nil? line)
        (.execute analyzer-thread-pool-executor
                  ^Runnable (fn []
                              (let [out-str (json/write-value-as-string
                                              (analysis-fn line analyzer))]
                                (.execute writer-thread-pool-executor
                                          ^Runnable (fn [] (.println writer out-str))))))
        (recur (.readLine rdr))))))

(defn ordered-analysis
  [reader ^PrintWriter writer analysis-fn analyzer
   ^ExecutorService analyzer-thread-pool-executor
   ^ExecutorService writer-thread-pool-executor]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)]
      (when-not (nil? line)
        (let [f (.submit analyzer-thread-pool-executor
                         ^Callable (fn []
                                     (json/write-value-as-string
                                       (analysis-fn line analyzer))))]
          (.execute writer-thread-pool-executor
                    ^Runnable (fn [] (.println writer (.get f)))))
        (recur (.readLine rdr))))))

(defn execute-analysis [files-to-analyze ^PrintWriter writer analyzer options]
  (let [preserve-order? (get options :preserve-order true)
        analysis-fn (if (get options :explain)
                      text-analysis/text->tokens
                      text-analysis/text->token-strings)
        reader-buffer-size (get options :reader-buffer-size 8192)
        queue-size (get options :queue-size 1024)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        ^ExecutorService analyzer-thread-pool-executor (c/thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (c/single-thread-executor)]
    (doseq [^String path files-to-analyze]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))]
        (if preserve-order?
          (ordered-analysis reader writer analysis-fn analyzer
                            analyzer-thread-pool-executor writer-thread-pool-executor)
          (unordered-analysis reader writer analysis-fn analyzer
                              analyzer-thread-pool-executor writer-thread-pool-executor))))
    (c/shutdown-thread-pool-executors analyzer-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))

(defn analyze-lines
  "Sequence of strings into sequence of text token sequences.
  Output format is valid JSON except when :graph true is provided.
  If given file path reads file otherwise stdin.

  Options:
  - :concurrency how many threads to use for text analysis.
  - :queue-size how lines to store in memory before processing them.
  - :explain should the token have the positional and other information.
  - :graph output would be a valid GraphViz.
  - :preserve-order should the output preserve the order of the input.
  - :reader-buffer-size in bytes.
  - :writer-buffer-size in bytes."
  [files-pattern files options]
  (let [print-writer-buffer-size (get options :writer-buffer-size 8192)
        analysis-conf (assoc (get options :analysis) :config-dir (get options :config-dir))
        files-to-analyze (if files-pattern
                           (into (fs/get-files files-pattern options)
                                 (fs/filter-files files))
                           [nil])
        custom-analyzers (analysis/prepare-analyzers (get options :analyzers-file) options)
        ^Analyzer analyzer (analyzer/create analysis-conf custom-analyzers)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size)
                                          ^Boolean (empty? files-pattern))]
    (if (get options :graph)
      (graph files-to-analyze writer analyzer options)
      (execute-analysis files-to-analyze writer analyzer options))))

(comment
  (lmgrep.only-analyze/analyze-lines
    "test/resources/test.txt"
    nil
    {:preserve-order true})

  (lmgrep.only-analyze/analyze-lines
    "test/resources/test.txt"
    nil
    {:preserve-order false})

  (lmgrep.only-analyze/analyze-lines
    "test/resources/test.txt"
    nil
    {:graph true}))
