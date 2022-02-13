(ns lmgrep.unordered
  (:require [clojure.java.io :as io]
            [lmgrep.matching :as matching])
  (:import (java.io BufferedReader PrintWriter BufferedWriter)
           (java.util.concurrent ThreadPoolExecutor TimeUnit Executors LinkedBlockingQueue ThreadPoolExecutor$CallerRunsPolicy ExecutorService)
           (lmgrep.matching LineNrStr)))

(defn shutdown-thread-pool-executors [& executors]
  (doseq [^ExecutorService executor executors]
    (.shutdown executor)
    (.awaitTermination executor 60 TimeUnit/SECONDS)))

(defn thread-pool-executor [^Integer concurrency ^Integer queue-size]
  (ThreadPoolExecutor.
    concurrency concurrency
    0 TimeUnit/MILLISECONDS
    (LinkedBlockingQueue. queue-size)
    (Executors/defaultThreadFactory)
    (ThreadPoolExecutor$CallerRunsPolicy.)))

(defn consume-reader
  "Given a Reader iterates over lines and sends them to the
  matcher-thread-pool-executor for further handling."
  [reader ^PrintWriter writer
   matcher-fn
   matcher-thread-pool-executor writer-thread-pool-executor
   with-empty-lines]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)
           line-nr 0]
      (when-not (nil? line)
        (.execute matcher-thread-pool-executor
                  ^Runnable (fn []
                              (let [^String out-str (matcher-fn (LineNrStr. line-nr line))]
                                (if (.equals "" out-str)
                                  (when with-empty-lines
                                    (.execute writer-thread-pool-executor
                                              ^Runnable (fn [] (.println writer out-str))))
                                  (.execute writer-thread-pool-executor
                                            ^Runnable (fn [] (.println writer out-str)))))))
        (recur (.readLine rdr) (inc line-nr))))))

(defn grep [file-paths-to-analyze highlighter-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        queue-size (get options :queue-size 1024)
        with-empty-lines (get options :with-empty-lines)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))
        ^ExecutorService matcher-thread-pool-executor (thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (Executors/newSingleThreadExecutor)]
    (if (empty? file-paths-to-analyze)
      (let [reader (BufferedReader. *in* reader-buffer-size)
            matcher-fn (matching/matcher-fn highlighter-fn nil options)]
        (consume-reader reader writer
                        matcher-fn
                        matcher-thread-pool-executor writer-thread-pool-executor
                        with-empty-lines))
      (doseq [^String path file-paths-to-analyze]
        (let [reader (io/reader path)
              matcher-fn (matching/matcher-fn highlighter-fn path options)]
          (consume-reader reader writer
                          matcher-fn
                          matcher-thread-pool-executor writer-thread-pool-executor
                          with-empty-lines))))
    (shutdown-thread-pool-executors matcher-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))
