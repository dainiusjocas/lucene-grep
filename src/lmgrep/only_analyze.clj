(ns lmgrep.only-analyze
  (:require [clojure.core.async :as a]
            [jsonista.core :as json]
            [lmgrep.analysis :as analysis]
            [lmgrep.fs :as fs]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.text-analysis :as text-analysis]
            [lmgrep.concurrent :as c])
  (:import (java.io BufferedReader PrintWriter BufferedWriter FileReader)
           (org.apache.lucene.analysis Analyzer)
           (java.util.concurrent ExecutorService)))

(set! *warn-on-reflection* true)

(defn only-analyze-ordered
  "Parallel processing pipeline that preserved the input order."
  [analyze-fn line-in-chan line-out-chan concurrency]
  (a/pipeline concurrency
              line-out-chan
              (map analyze-fn)
              line-in-chan
              true
              (fn [^Throwable t]
                (when (System/getenv "DEBUG_MODE")
                  (.printStackTrace t))
                (a/close! line-out-chan)
                (System/exit 1))))

(defn read-input-lines-to-channel
  "Starts a thread that reads strings from an input reader and puts them to a channel."
  [^BufferedReader input-reader channel]
  (a/go
    (with-open [^BufferedReader rdr input-reader]
      (loop [^String line (.readLine rdr)]
        (if (nil? line)
          (a/close! channel)
          (do
            (a/>! channel line)
            (recur (.readLine rdr))))))))

(defn write-output-from-channel
  "Write data from a channel to the PrintWriter. Intended to be run on the main thread."
  [^PrintWriter writer channel]
  (loop [^String line (a/<!! channel)]
    (when-not (nil? line)
      (.println writer line)
      (recur (a/<!! channel))))
  (.flush writer))

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

(defn unordered [files-to-analyze ^PrintWriter writer analyzer analysis-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        queue-size (get options :queue-size 1024)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        ^ExecutorService analyzer-thread-pool-executor (c/thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (c/single-thread-executor)]
    (doseq [^String path files-to-analyze]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))]
        (unordered-analysis reader writer analysis-fn analyzer
                            analyzer-thread-pool-executor writer-thread-pool-executor)))
    (c/shutdown-thread-pool-executors analyzer-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))

(defn ordered-analysis [reader writer analysis-fn analyzer concurrency queue-size]
  (let [line-in-chan (a/chan queue-size)
        line-out-chan (a/chan queue-size)
        analyze-fn (fn [line] (json/write-value-as-string (analysis-fn line analyzer)))]
    (only-analyze-ordered analyze-fn line-in-chan line-out-chan concurrency)
    (read-input-lines-to-channel reader line-in-chan)
    (write-output-from-channel writer line-out-chan)))

(defn analyze-to-graph [input-reader ^PrintWriter writer analyzer]
  (with-open [^BufferedReader rdr input-reader]
    (loop [^String line (.readLine rdr)]
      (if (nil? line)
        (.flush writer)
        (do
          (.println writer (text-analysis/text->graph line analyzer))
          (recur (.readLine rdr)))))))

(defn ordered [files-to-analyze writer analyzer analysis-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        queue-size (get options :queue-size 1024)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))]
    (doseq [^String path files-to-analyze]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))]
        (ordered-analysis reader writer analysis-fn analyzer concurrency queue-size)))))

(defn graph [files-to-analyze writer analyzer options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)]
    (doseq [^String path files-to-analyze]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))]
        (analyze-to-graph reader writer analyzer)))))

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
        preserve-order? (get options :preserve-order true)
        analysis-conf (assoc (get options :analysis) :config-dir (get options :config-dir))
        analysis-fn (if (get options :explain)
                      text-analysis/text->tokens
                      text-analysis/text->token-strings)
        files-to-analyze (if files-pattern
                           (into (fs/get-files files-pattern options)
                                 (fs/filter-files files))
                           [nil])
        custom-analyzers (analysis/prepare-analyzers (get options :analyzers-file) options)
        ^Analyzer analyzer (analyzer/create analysis-conf custom-analyzers)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))]
    (if (get options :graph)
      (graph files-to-analyze writer analyzer options)
      (if preserve-order?
        (ordered files-to-analyze writer analyzer analysis-fn options)
        (unordered files-to-analyze writer analyzer analysis-fn options)))))

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
