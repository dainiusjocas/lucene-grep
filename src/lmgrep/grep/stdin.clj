(ns lmgrep.grep.stdin
  (:require [jsonista.core :as json]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene :as lucene])
  (:import (java.io BufferedWriter PrintWriter BufferedReader)
           (java.util.concurrent ExecutorService Executors TimeUnit)))

(set! *warn-on-reflection* true)

(defn ordered-grep []
  (println "TODO IMPLEMENT ORDERED"))

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defn matcher-fn
  "Creates a function that given the line and its number forms the highlighted line."
  [highlighter-fn file-path options]
  ;; This function can not return nil values
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (get options :with-details)
        format (get options :format)
        scored? (or (get options :with-score)
                    (get options :with-scored-highlights))]
    (fn [^String line-str line-nr]
      (if-let [highlights (seq (highlighter-fn line-str highlight-opts))]
        (let [details (cond-> {:line-number line-nr
                               :line        line-str}
                              file-path (assoc :file file-path)
                              (true? scored?) (assoc :score (sum-score highlights))
                              (true? with-details?) (assoc :highlights highlights))]
          (case format
            :edn (pr-str details)
            :json (json/write-value-as-string details)
            :string (formatter/string-output highlights details options)
            (formatter/string-output highlights details options)))
        ""))))

(defn unordered-grep [reader ^PrintWriter writer grep-fn concurrency with-empty-lines]
  (let [^ExecutorService grep-pool (Executors/newFixedThreadPool concurrency)
        ^ExecutorService writer-pool (Executors/newSingleThreadExecutor)]
    (with-open [^BufferedReader rdr reader]
      (loop [^String line (.readLine rdr)
             line-nr 1]
        (when-not (nil? line)
          (.submit grep-pool
                   ^Runnable (fn []
                               (let [out-str (grep-fn line line-nr)]
                                 (.submit writer-pool
                                          ^Runnable (fn []
                                                      (if (.equals "" out-str)
                                                        (when with-empty-lines (.println writer))
                                                        (.println writer out-str)))))))
          (recur (.readLine rdr) (inc line-nr))))
      (.shutdown grep-pool)
      (.awaitTermination grep-pool 60 TimeUnit/SECONDS)
      (.shutdown writer-pool)
      (.awaitTermination writer-pool 60 TimeUnit/SECONDS)
      (.flush writer))))

(defn grep [highlighter-fn options]
  (let [reader-buffer-size (get options :reader-buffer-size (* 2 1024 8192))
        print-writer-buffer-size (get options :writer-buffer-size (* 8192 8192))
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        preserve-order? (get options :preserve-order true)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))
        ^BufferedReader reader (BufferedReader. *in* reader-buffer-size)
        grep-fn (matcher-fn highlighter-fn nil options)
        with-empty-lines (get options :with-empty-lines false)]
    (if preserve-order?
      (ordered-grep)
      (unordered-grep reader writer grep-fn concurrency with-empty-lines))))

(defn -main [& query]
  (println "START")
  (grep (lucene/highlighter [{:query (first query)}] {:preserve-order false})
        {:preserve-order false})
  (println "DONE"))
