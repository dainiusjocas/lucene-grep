(ns lmgrep.only-analyze
  (:require [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.analysis-conf :as ac]
            [clojure.core.async :as a]
            [lmgrep.lucene.text-analysis :as text-analysis]
            [lmgrep.fs :as fs]
            [jsonista.core :as json]
            [clojure.java.io :as io])
  (:import (java.io BufferedReader PrintWriter BufferedWriter)
           (org.apache.lucene.analysis Analyzer)))


(defn only-analyze-unordered
  "Given a line-in-chan that contains strings to analyze, passes every string to the analyze-fn whose
  output is a JSON-encoded string. The results of the analyze-fn are put on the line-out-chan."
  [analyze-fn line-in-chan line-out-chan concurrency]
  (let [not-done (atom concurrency)]
    (dotimes [_ concurrency]
      (a/thread
        (if-let [^String line (a/<!! line-in-chan)]
          (do
            (try
              (a/>!! line-out-chan (analyze-fn line))
              (catch Throwable t
                (when (System/getenv "DEBUG_MODE")
                  (.printStackTrace t))
                (a/close! line-out-chan)
                (System/exit 1)))
            (recur))
          ; when all threads are done close the output channel, that marks the end of processing
          (when (zero? (swap! not-done dec))
            (a/close! line-out-chan)))))))

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
  (a/thread
    (with-open [^BufferedReader rdr input-reader]
      (loop [^String line (.readLine rdr)]
        (if (nil? line)
          (a/close! channel)
          (do
            (a/>!! channel line)
            (recur (.readLine rdr))))))))

(defn write-output-from-channel
  "Write to data from a channel to PrintWriter on the main thread."
  [^PrintWriter writer channel]
  (loop [^String line (a/<!! channel)]
    (when-not (nil? line)
      (.println writer line)
      (recur (a/<!! channel))))
  (.flush writer))

(defn analyze-lines
  "Sequence of text into sequence of text token sequences. Output format is JSON.
  If given file path reads file otherwise stdin."
  [files-pattern files options]
  (let [line-in-buffer-size (* 1024 8)
        line-out-buffer-size (* 1024 8)
        reader-buffer-size (* 1024 8192)
        print-writer-buffer-size (* 8192 8192)
        preserve-order? (get options :preserve-order true)
        concurrency (get options :concurrency (.availableProcessors (Runtime/getRuntime)))
        analysis-conf (ac/prepare-analysis-configuration ac/default-text-analysis options)
        analysis-fn (if (get options :explain)
                      text-analysis/text->tokens
                      text-analysis/text->token-strings)
        files-to-analyze (if files-pattern
                           (into (fs/get-files files-pattern options)
                                 (fs/filter-files files))
                           [nil])
        ^Analyzer analyzer (analyzer/create analysis-conf)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* print-writer-buffer-size))]
    (doseq [path files-to-analyze]
      (let [line-in-chan (a/chan line-in-buffer-size)
            line-out-chan (a/chan line-out-buffer-size)
            analyze-fn (fn [line] (json/write-value-as-string (analysis-fn line analyzer)))]
        (if preserve-order?
          (only-analyze-ordered analyze-fn line-in-chan line-out-chan concurrency)
          (only-analyze-unordered analyze-fn line-in-chan line-out-chan concurrency))
        (read-input-lines-to-channel (if path
                                       (io/reader path)
                                       (BufferedReader. *in* reader-buffer-size))
                                     line-in-chan)
        (write-output-from-channel writer line-out-chan)))))

(comment
  (lmgrep.only-analyze/analyze-lines
    "test/resources/test.txt"
    nil
    {}))
