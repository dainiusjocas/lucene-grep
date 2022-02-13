(ns lmgrep.only-analyze
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [lmgrep.analysis :as analysis]
            [lmgrep.fs :as fs]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (java.io BufferedReader PrintWriter BufferedWriter)
           (org.apache.lucene.analysis Analyzer)
           (java.util.concurrent ExecutorService Executors TimeUnit
                                 LinkedBlockingQueue ThreadPoolExecutor
                                 ThreadPoolExecutor$CallerRunsPolicy)))

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
  [reader ^PrintWriter writer analysis-fn analyzer concurrency queue-size]
  (let [^ExecutorService analyzer-pool (ThreadPoolExecutor.
                                         concurrency concurrency
                                         0 TimeUnit/MILLISECONDS
                                         (LinkedBlockingQueue. ^Integer queue-size)
                                         (Executors/defaultThreadFactory)
                                         (ThreadPoolExecutor$CallerRunsPolicy.))
        ^ExecutorService writer-pool (Executors/newSingleThreadExecutor)]
    (with-open [^BufferedReader rdr reader]
      (loop [^String line (.readLine rdr)]
        (when-not (nil? line)
          (.execute analyzer-pool
                    ^Runnable (fn []
                                (let [out-str (json/write-value-as-string
                                                (analysis-fn line analyzer))]
                                  (.execute writer-pool
                                            ^Runnable (fn [] (.println writer out-str))))))
          (recur (.readLine rdr))))
      (.shutdown analyzer-pool)
      (.awaitTermination analyzer-pool 60 TimeUnit/SECONDS)
      (.shutdown writer-pool)
      (.awaitTermination writer-pool 60 TimeUnit/SECONDS)
      (.flush writer))))

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
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        queue-size (get options :queue-size 1024)
        preserve-order? (get options :preserve-order true)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
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
    (doseq [path files-to-analyze]
      (let [reader (if path
                     (io/reader path)
                     (BufferedReader. *in* reader-buffer-size))]
        (if (get options :graph)
          (analyze-to-graph reader writer analyzer)
          (if preserve-order?
            (ordered-analysis reader writer analysis-fn analyzer concurrency queue-size)
            (unordered-analysis reader writer analysis-fn analyzer concurrency queue-size)))))))

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
