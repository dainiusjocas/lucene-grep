(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as impl]
            [jsonista.core :as json]
            [lmgrep.fs :as fs]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene :as lucene]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.analysis-conf :as ac]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (java.io BufferedReader File PrintWriter BufferedWriter)
           (org.apache.lucene.analysis Analyzer)))

(set! *warn-on-reflection* true)

(defn sum-score [highlights]
  (when-let [scores (seq (remove nil? (map :score highlights)))]
    (reduce + scores)))

(defn seq-of-chan
  "Creates a lazy seq from a core.async channel."
  [c]
  (lazy-seq
    (let [fst (a/<!! c)]
      (if (nil? fst) nil (cons fst (seq-of-chan c))))))

(defn map-pipeline
  "Parallel map for compute intensive functions, backed by clojure.core.async/pipeline."
  ([f p coll]
   (let [ic (a/chan p)
         oc (a/chan p)]
     (a/onto-chan! ic coll)
     (a/pipeline (min p impl/MAX-QUEUE-SIZE)
                 oc
                 (map f)
                 ic
                 true
                 (fn [^Throwable t]
                   (.println System/err (format "Failed with: '%s'" (.toString t)))
                   (a/close! oc)
                   (System/exit 1)))
     (seq-of-chan oc)))
  ([f coll] (map-pipeline f 16 coll)))

(defrecord LineNrStr [nr str])

(defn matcher-fn [highlighter-fn file-path options]
  ;; This function can not return nil values
  (let [highlight-opts (select-keys options [:with-score :with-scored-highlights])
        with-details? (:with-details options)
        format (:format options)
        scored? (or (:with-score options) (:with-scored-highlights options))]
    (fn [^LineNrStr line-nr-and-line-str]
      (if-let [highlights (seq (highlighter-fn (.str line-nr-and-line-str) highlight-opts))]
        (let [details (cond-> {:line-number (inc (.nr line-nr-and-line-str))
                               :line        (.str line-nr-and-line-str)}
                              file-path (assoc :file file-path)
                              (true? scored?) (assoc :score (sum-score highlights))
                              (true? with-details?) (assoc :highlights highlights))]
          (case format
            :edn (pr-str details)
            :json (json/write-value-as-string details)
            :string (formatter/string-output highlights details options)
            (formatter/string-output highlights details options)))
        ""))))

(defn match-lines [highlighter-fn file-path lines options]
  (let [parallel-matcher (matcher-fn highlighter-fn file-path options)
        concurrency (get options :concurrency 8)
        numbered-lines (map-indexed (fn [line-str line-number] (LineNrStr. line-str line-number)) lines)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out*))
        with-empty-lines (:with-empty-lines options)]
    (doseq [^String to-print (map-pipeline parallel-matcher concurrency numbered-lines)]
      (if (.equals "" to-print)
        (when with-empty-lines (.println writer))
        (.println writer to-print)))
    (.flush writer)))

(defn read-questionnaire-from-file [^String file-path]
  (let [^File input-file (io/file file-path)]
    (if (.isFile input-file)
      (with-open [is (io/input-stream input-file)]
        (json/read-value is json/keyword-keys-object-mapper))
      (throw (Exception. (format "File '%s' doesn't exist." file-path))))))

(defn combine-questionnaire [lucene-query-strings options]
  (into (mapv (fn [lucene-query-string] {:query lucene-query-string
                                         :query-parser (get options :query-parser)})
              lucene-query-strings)
        (when-let [queries-file-path (:queries-file options)]
          (read-questionnaire-from-file queries-file-path))))

(defn grep [lucene-query-strings files-pattern files options]
  (let [questionnaire (combine-questionnaire lucene-query-strings options)
        highlighter-fn (lucene/highlighter questionnaire options)]
    (if files-pattern
      (doseq [path (into (fs/get-files files-pattern options)
                         (fs/filter-files files))]
        (if (:split options)
          (with-open [rdr (io/reader path)]
            (match-lines highlighter-fn path (line-seq rdr) options))
          (match-lines highlighter-fn path [(slurp path)] options)))
      (if (:split options)
        (match-lines highlighter-fn nil (line-seq (BufferedReader. *in*)) options)
        (match-lines highlighter-fn nil [(str/trim (slurp *in*))] options)))))

(comment
  (lmgrep.grep/grep ["opt"] "**.md" nil {:format :edn})

  (lmgrep.grep/grep ["test" "opt"] "**.md" nil {:split true})

  (time (lmgrep.grep/grep ["opt"] "**.class" nil {:format            :edn
                                                  :skip-binary-files true})))

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
        ^Analyzer analyzer (analyzer/create analysis-conf)
        analysis-fn (if (get options :explain)
                      text-analysis/text->tokens
                      text-analysis/text->token-strings)
        files-to-analyze (if files-pattern
                           (into (fs/get-files files-pattern options)
                                 (fs/filter-files files))
                           [nil])
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
  (lmgrep.grep/analyze-lines
    "test/resources/test.txt"
    nil
    {}))
