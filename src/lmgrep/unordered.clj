(ns lmgrep.unordered
  (:require [lmgrep.concurrent :as c]
            [lmgrep.matching :as matching])
  (:import (java.io BufferedReader PrintWriter BufferedWriter FileReader)
           (java.util.concurrent ExecutorService)))

(set! *warn-on-reflection* true)

(defn unordered-consume-reader
  "Given a Reader iterates over lines and sends them to the
  matcher-thread-pool-executor for further handling."
  [reader matcher-fn
   ^ExecutorService matcher-thread-pool-executor
   ^ExecutorService writer-thread-pool-executor
   ^PrintWriter writer
   with-empty-lines]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)
           line-nr 1]
      (when-not (nil? line)
        (.execute matcher-thread-pool-executor
                  ^Runnable (fn []
                              (let [^String out-str (matcher-fn line-nr line)]
                                (if out-str
                                  (.execute writer-thread-pool-executor
                                            ^Runnable (fn [] (.println writer out-str)))
                                  (when with-empty-lines
                                    (.execute writer-thread-pool-executor
                                              ^Runnable (fn [] (.println writer))))))))
        (recur (.readLine rdr) (inc line-nr))))))

(defn ordered-consume-reader
  "Given a Reader iterates over lines and sends them to the
  matcher-thread-pool-executor for further handling."
  [reader matcher-fn
   ^ExecutorService matcher-thread-pool-executor
   ^ExecutorService writer-thread-pool-executor
   ^PrintWriter writer
   with-empty-lines]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)
           line-nr 1]
      (when-not (nil? line)
        (let [f (.submit matcher-thread-pool-executor
                         ^Callable (fn [] (matcher-fn line-nr line)))]
          (.execute writer-thread-pool-executor
                    ^Runnable (fn [] (let [out-str (.get f)]
                                       (if out-str
                                         (.println writer out-str)
                                         (when with-empty-lines
                                           (.println writer)))))))
        (recur (.readLine rdr) (inc line-nr))))))

(defn grep [file-paths-to-analyze highlighter-fn options]
  (let [preserve-order? (get options :preserve-order true)
        reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        queue-size (get options :queue-size 1024)
        with-empty-lines (get options :with-empty-lines)
        consume-fn (if preserve-order? ordered-consume-reader unordered-consume-reader)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size)
                                          ^Boolean (empty? file-paths-to-analyze))
        ^ExecutorService matcher-thread-pool-executor (c/thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (c/single-thread-executor)]
    (doseq [^String path (if (empty? file-paths-to-analyze)
                           [nil]                            ;; STDIN is an input
                           file-paths-to-analyze)]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))
            matcher-fn (matching/matcher-fn highlighter-fn path options)]
        (consume-fn reader
                    matcher-fn
                    matcher-thread-pool-executor
                    writer-thread-pool-executor
                    writer
                    with-empty-lines)))
    (c/shutdown-thread-pool-executors matcher-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))




(defn batched-consume-reader
  "Given a Reader iterates over lines and sends them to the
  matcher-thread-pool-executor for further handling."
  [reader matcher-fn
   ^ExecutorService matcher-thread-pool-executor
   ^ExecutorService writer-thread-pool-executor
   ^PrintWriter writer
   batch-size]
  (with-open [^BufferedReader rdr reader]
    (loop [^String line (.readLine rdr)
           lines (transient [])]
      (if-not (nil? line)
        (if (< (count lines) batch-size)
          (recur (.readLine rdr) (conj! lines line))

          (do
            (let [f (.submit matcher-thread-pool-executor
                             ^Callable (fn [] (matcher-fn (persistent! lines))))]
              (.execute writer-thread-pool-executor
                        ^Runnable (fn [] (let [lines-to-print (.get f)]
                                           (when lines-to-print
                                             (.println writer lines-to-print))))))
            (recur (.readLine rdr) (transient [line]))))

        (let [f (.submit matcher-thread-pool-executor
                         ^Callable (fn [] (matcher-fn (persistent! lines))))]
          (.execute writer-thread-pool-executor
                    ^Runnable (fn [] (let [lines-to-print (.get f)]
                                       (when lines-to-print
                                         (.println writer lines-to-print))))))))))

(defn batched-grep [file-paths-to-analyze highlighter-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size 8192)
        print-writer-buffer-size (get options :writer-buffer-size 8192)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        queue-size (get options :queue-size 1024)
        batch-size (get options :batch-size 1000)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size)
                                          ^Boolean (empty? file-paths-to-analyze))
        ^ExecutorService matcher-thread-pool-executor (c/thread-pool-executor concurrency queue-size)
        ^ExecutorService writer-thread-pool-executor (c/single-thread-executor)]
    (doseq [^String path (if (empty? file-paths-to-analyze)
                           [nil]                            ;; STDIN is an input
                           file-paths-to-analyze)]
      (let [reader (if path
                     (BufferedReader. (FileReader. path) reader-buffer-size)
                     (BufferedReader. *in* reader-buffer-size))
            matcher-fn (matching/batched-matcher-fn highlighter-fn path options)]
        (batched-consume-reader reader
                                matcher-fn
                                matcher-thread-pool-executor
                                writer-thread-pool-executor
                                writer
                                batch-size)))
    (c/shutdown-thread-pool-executors matcher-thread-pool-executor writer-thread-pool-executor)
    (.flush writer)))
