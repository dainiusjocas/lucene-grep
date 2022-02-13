(ns lmgrep.concurrent
  (:import (java.util.concurrent ExecutorService Executors
                                 ThreadPoolExecutor ThreadPoolExecutor$CallerRunsPolicy
                                 LinkedBlockingQueue TimeUnit)))

(defn shutdown-thread-pool-executors [& executors]
  (doseq [^ExecutorService executor executors]
    (.shutdown executor)
    (.awaitTermination executor 60 TimeUnit/SECONDS)))

(defn single-thread-executor []
  (Executors/newSingleThreadExecutor))

(defn thread-pool-executor [^Integer concurrency ^Integer queue-size]
  (ThreadPoolExecutor.
    concurrency concurrency
    0 TimeUnit/MILLISECONDS
    (LinkedBlockingQueue. queue-size)
    (Executors/defaultThreadFactory)
    (ThreadPoolExecutor$CallerRunsPolicy.)))
