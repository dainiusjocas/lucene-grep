(ns lmgrep.unordered
  (:require [lmgrep.concurrent :as c]
            [lmgrep.matching :as matching])
  (:import (java.io BufferedReader PrintWriter BufferedWriter FileReader)
           (java.util.concurrent ExecutorService)
           (lmgrep.matching LineNrStr)))

(defn consume-reader
  "Given a Reader iterates over lines and sends them to the
  matcher-thread-pool-executor for further handling."
  [reader matcher-fn matcher-thread-pool-executor]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)
           line-nr 0]
      (when-not (nil? line)
        (.execute matcher-thread-pool-executor ^Runnable (matcher-fn line-nr line))
        (recur (.readLine rdr) (inc line-nr))))))

(defn create-unordered-matcher-fn
  [matcher-fn writer-thread-pool-executor writer with-empty-lines]
  (fn [line-nr line]
    (fn []
      (let [^String out-str (matcher-fn (LineNrStr. line-nr line))]
        (if (.equals "" out-str)
          (when with-empty-lines
            (.execute writer-thread-pool-executor
                      ^Runnable (fn [] (.println writer out-str))))
          (.execute writer-thread-pool-executor
                    ^Runnable (fn [] (.println writer out-str))))))))

(defn grep [file-paths-to-analyze highlighter-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        queue-size (get options :queue-size 1024)
        with-empty-lines (get options :with-empty-lines)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))
        ^ExecutorService matcher-thread-pool-executor (c/thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (c/single-thread-executor)]
    (doseq [^String path (if (empty? file-paths-to-analyze)
                           [nil]                            ;; STDIN is an input
                           file-paths-to-analyze)]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))
            matcher-fn (matching/matcher-fn highlighter-fn path options)
            unordered-matcher-fn (create-unordered-matcher-fn matcher-fn
                                                              writer-thread-pool-executor
                                                              writer
                                                              with-empty-lines)]
        (consume-reader reader unordered-matcher-fn matcher-thread-pool-executor)))
    (c/shutdown-thread-pool-executors matcher-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))
