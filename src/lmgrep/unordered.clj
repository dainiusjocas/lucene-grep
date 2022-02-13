(ns lmgrep.unordered
  (:require [clojure.java.io :as io]
            [lmgrep.matching :as matching])
  (:import (java.io BufferedReader PrintWriter BufferedWriter)
           (java.util.concurrent ThreadPoolExecutor TimeUnit Executors LinkedBlockingQueue ThreadPoolExecutor$CallerRunsPolicy ExecutorService)
           (lmgrep.matching LineNrStr)))

(defn consume-reader
  "TODO:"
  [reader ^PrintWriter writer matcher-fn concurrency queue-size with-empty-lines]
  (let [^ExecutorService analyzer-pool (ThreadPoolExecutor.
                                         concurrency concurrency
                                         0 TimeUnit/MILLISECONDS
                                         (LinkedBlockingQueue. ^Integer queue-size)
                                         (Executors/defaultThreadFactory)
                                         (ThreadPoolExecutor$CallerRunsPolicy.))
        ^ExecutorService writer-pool (Executors/newSingleThreadExecutor)]
    (with-open [^BufferedReader rdr reader]
      (loop [^String line (.readLine rdr)
             line-nr 0]
        (when-not (nil? line)
          (.execute analyzer-pool
                    ^Runnable (fn []
                                (let [out-str (matcher-fn (LineNrStr. line-nr line))]
                                  (if (.equals "" out-str)
                                    (when with-empty-lines
                                      (.execute writer-pool
                                                ^Runnable (fn [] (.println writer out-str))))
                                    (.execute writer-pool
                                              ^Runnable (fn [] (.println writer out-str)))))))
          (recur (.readLine rdr) (inc line-nr))))
      (.shutdown analyzer-pool)
      (.awaitTermination analyzer-pool 60 TimeUnit/SECONDS)
      (.shutdown writer-pool)
      (.awaitTermination writer-pool 60 TimeUnit/SECONDS)
      (.flush writer))))

(defn grep [file-paths-to-analyze highlighter-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        queue-size (get options :queue-size 1024)
        with-empty-lines (get options :with-empty-lines)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))]
    (if (empty? file-paths-to-analyze)
      (let [reader (BufferedReader. *in* reader-buffer-size)
            matcher-fn (matching/matcher-fn highlighter-fn nil options)]
        (consume-reader reader writer matcher-fn concurrency queue-size with-empty-lines))
      (doseq [^String path file-paths-to-analyze]
        (let [reader (io/reader path)
              matcher-fn (matching/matcher-fn highlighter-fn path options)]
          (consume-reader reader writer matcher-fn concurrency queue-size with-empty-lines))))))
