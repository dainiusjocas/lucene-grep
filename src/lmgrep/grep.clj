(ns lmgrep.grep
  (:require [clojure.java.io :as io]
            [jsonista.core :as json]
            [lmgrep.fs :as fs]
            [lmgrep.lucene :as lucene]
            [lmgrep.analysis :as analysis]
            [lmgrep.unordered :as unordered])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

(defn read-questionnaire-from-file [^String file-path]
  (let [^File input-file (io/file file-path)]
    (if (.isFile input-file)
      (with-open [is (io/input-stream input-file)]
        (json/read-value is json/keyword-keys-object-mapper))
      (throw (Exception. (format "File '%s' doesn't exist." file-path))))))

(def DEFAULT_QUERY_PARSER :classic)
(def DEFAULT_QUERY_PARSER_CONF {:allow-leading-wildcard true})

(defn combine-questionnaire [lucene-query-strings options]
  (into (mapv (fn [lucene-query-string] {:query             lucene-query-string
                                         :query-parser      (get options :query-parser DEFAULT_QUERY_PARSER)
                                         :query-parser-conf (merge DEFAULT_QUERY_PARSER_CONF
                                                                   (get options :query-parser-conf))})
              lucene-query-strings)
        (when-let [queries-file-path (get options :queries-file)]
          (read-questionnaire-from-file queries-file-path))))

(defn grep [lucene-query-strings files-pattern files options]
  (let [questionnaire (combine-questionnaire lucene-query-strings options)
        custom-analyzers (analysis/prepare-analyzers (get options :analyzers-file) options)
        file-paths-to-analyze (into (fs/get-files files-pattern options)
                                    (fs/filter-files files))]
    (with-open [highlighter (lucene/highlighter-obj questionnaire options custom-analyzers)]
      (unordered/grep file-paths-to-analyze highlighter options))))

(comment
  (lmgrep.grep/grep ["opt"] "**.md" nil {:format :edn})

  (lmgrep.grep/grep ["test" "opt"] "**.md" nil {:split true})

  (time (lmgrep.grep/grep ["opt"] "**.class" nil {:format            :edn
                                                  :skip-binary-files true})))
