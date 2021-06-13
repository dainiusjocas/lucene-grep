(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [clojure.core.async.impl.protocols :as impl]
            [clojure.core.reducers :as r]
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
        concurrency 1
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
      (doseq [path (concat (fs/get-files files-pattern options)
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

(defn analyze-lines
  "Sequence of text into sequence of text token sequences. Output format is JSON.
  If given file path reads file otherwise stdin."
  [files-pattern files options]
  (let [analysis-conf (ac/prepare-analysis-configuration ac/default-text-analysis options)
        ^Analyzer analyzer (analyzer/create analysis-conf)
        ^PrintWriter writer (PrintWriter. (BufferedWriter. *out* (* 1024 8192)))
        analysis-fn (if (get options :explain)
                      text-analysis/text->tokens
                      text-analysis/text->token-strings)
        fta (if files-pattern
              (into (fs/get-files files-pattern options)
                    (fs/filter-files files))
              [nil])]
    (doseq [path fta]
      (let [line-in-chan (a/chan 1024)
            line-out-chan (a/chan (* 2 1024))]

        ;; parallel processing pipeline on a threadpool
        (a/pipeline (* 4 (.availableProcessors (Runtime/getRuntime)))
                    line-out-chan
                    (map (fn [line]
                           (json/write-value-as-string
                             (analysis-fn line analyzer))))
                    line-in-chan
                    true
                    (fn [^Throwable t]
                      (when (System/getenv "DEBUG_MODE")
                        (.printStackTrace t))
                      (a/close! line-out-chan)
                      (System/exit 1)))

        ;; read lines in a thread pool
        (a/go
          (with-open [^BufferedReader rdr (if path
                                            (io/reader path)
                                            (BufferedReader. *in* (* 1024 8192)))]
            (loop [^String line (.readLine rdr)]
              (if (= nil line)
                (a/close! line-in-chan)
                (do
                  (a/>!! line-in-chan line)
                  (recur (.readLine rdr)))))))

        ;; write to stdout on the main thread
        (loop [^String line (a/<!! line-out-chan)]
          (when-not (= nil line)
            (.println writer line)
            (recur (a/<!! line-out-chan))))
        (.flush writer)))))

(comment
  (lmgrep.grep/analyze-lines
    "test/resources/test.txt"
    nil
    {}))
