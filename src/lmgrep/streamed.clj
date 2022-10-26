(ns lmgrep.streamed
  (:require [jsonista.core :as json]
            [lmgrep.io :as io]
            [lmgrep.lucene :as lucene]
            [lmgrep.analysis :as analysis]
            [lmgrep.matching :as matching]
            [lmgrep.concurrent :as c])
  (:import (java.io BufferedReader BufferedWriter PrintWriter)
           (java.util.concurrent ExecutorService)
           (lmgrep.lucene LuceneMonitorMatcher)))

(set! *warn-on-reflection* true)

(defn safe-json-parse [^String json-string]
  (try
    (json/read-value json-string)
    (catch Exception e
      (when (Boolean/parseBoolean (System/getenv "DEBUG_MODE"))
        (.printStackTrace e))
      (io/println-to-err (.getMessage e)))))

(defn wrapped-matcher-fn [custom-analyzers options]
  (fn [^long line-nr ^String line]
    (let [task (safe-json-parse line)
          query (get task "query")
          text (get task "text")]
      (when (and query text)
        (with-open [^LuceneMonitorMatcher highlighter
                    (lucene/highlighter-obj [{:query query}] options custom-analyzers)]
          ((matching/matcher-fn highlighter nil options) line-nr text))))))

(defn unordered [reader ^ExecutorService matcher-thread-pool-executor
                 ^PrintWriter writer ^ExecutorService writer-thread-pool-executor
                 with-empty-lines custom-analyzers options]
  (let [matcher-fn (wrapped-matcher-fn custom-analyzers options)]
    (with-open [^BufferedReader rdr reader]
      (loop [^String line (.readLine rdr)
             line-nr 1]
        (when-not (nil? line)
          (.execute matcher-thread-pool-executor
                    ^Runnable (fn []
                                (if-let [out-str (matcher-fn line-nr line)]
                                  (.execute writer-thread-pool-executor
                                            ^Runnable (fn [] (io/print-to-writer writer out-str)))
                                  (when with-empty-lines
                                    (.execute writer-thread-pool-executor
                                              ^Runnable (fn [] (io/print-to-writer writer)))))))
          (recur (.readLine rdr) (inc line-nr)))))))

(defn ordered [reader ^ExecutorService matcher-thread-pool-executor
               ^PrintWriter writer ^ExecutorService writer-thread-pool-executor
               with-empty-lines custom-analyzers options]
  (let [matcher-fn (wrapped-matcher-fn custom-analyzers options)]
    (with-open [^BufferedReader rdr reader]
      (loop [^String line (.readLine rdr)
             line-nr 1]
        (when-not (nil? line)
          (let [f (.submit matcher-thread-pool-executor
                           ^Callable (fn [] (matcher-fn line-nr line)))]
            (.execute writer-thread-pool-executor
                      ^Runnable (fn []
                                  (let [^String out-str (.get f)]
                                    (if out-str
                                      (io/print-to-writer writer out-str)
                                      (when with-empty-lines
                                        (io/print-to-writer writer)))))))
          (recur (.readLine rdr) (inc line-nr)))))))

(defn grep
  "Listens on STDIN where every line should include JSON with both: query and the text.
  Example input: {\"query\": \"nike~\", \"text\": \"I am selling nikee\"}

  If either query or text is not present in the JSON, then matching is skipped.

  When a bad JSON string is passed then program doesn't crash:
    1. ThreadPoolExecutor is async and it 'swallows' Exceptions.
    2. Upstream errors happen and we should handle them."
  [options]
  (let [custom-analyzers (analysis/prepare-analyzers (get options :analyzers-file) options)
        reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        ^BufferedReader reader (BufferedReader. *in* reader-buffer-size)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size) true)
        with-empty-lines (get options :with-empty-lines)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        queue-size (get options :queue-size 1024)
        ^ExecutorService matcher-thread-pool-executor (c/thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (c/single-thread-executor)
        preserve-order? (get options :preserve-order true)
        consume-fn (if preserve-order? ordered unordered)]
    (consume-fn reader matcher-thread-pool-executor
                writer writer-thread-pool-executor
                with-empty-lines custom-analyzers options)
    (c/shutdown-thread-pool-executors matcher-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))
