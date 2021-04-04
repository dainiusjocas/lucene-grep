(ns lmgrep.lucene.dictionary
  (:require [clojure.core.reducers :as r]
            [lmgrep.lucene.text-analysis :as text-analysis])
  (:import (org.apache.lucene.queryparser.classic QueryParser ParseException)
           (org.apache.lucene.monitor MonitorQuery)
           (org.apache.lucene.search Query)
           (org.apache.lucene.analysis Analyzer)))

(defn prepare-metadata
  "Metadata must be a map String->String"
  [type meta]
  (let [str->str (if meta
                   (reduce-kv (fn [m k v] (assoc m (name k) v)) {} meta)
                   {})]
    (assoc str->str "_type" type)))

(defn query->monitor-query [dict-entry field-name monitor-analyzer]
  (try
    (MonitorQuery. ^String (get dict-entry :id)
                   ^Query (.parse (QueryParser. ^String field-name ^Analyzer monitor-analyzer)
                                  ^String (get dict-entry :query))
                   ^String (get dict-entry :query)
                   (prepare-metadata (get dict-entry :type) (get dict-entry :meta)))
    (catch ParseException e
      (.println System/err (format "Failed to parse query: '%s' with exception '%s'" dict-entry e)))
    (catch Exception e
      (.println System/err (format "Failed create query: '%s' with '%s'" dict-entry e)))))

(def default-text-analysis
  (text-analysis/map->Conf
    {:case-sensitive?             false
     :ascii-fold?                 true
     :stem?                       true
     :tokenizer                   :standard
     :stemmer                     :english
     :word-delimiter-graph-filter 0}))

(defn normalize-dictionary-entry [dictionary-entry default-type]
  (-> dictionary-entry
      (update :stemmer keyword)
      (update :tokenizer keyword)
      (update :type (fn [type] (if type type default-type)))))

(defn inject-analysis-options [dictionary-entry analysis-keys analysis-options]
  (reduce (fn [dict-entry k]
            (assoc dict-entry k (text-analysis/two-way-merge k analysis-options dict-entry)))
          dictionary-entry
          analysis-keys))

(def analysis-keys (keys default-text-analysis))

(defrecord Dict [id query type meta
                 tokenizer case-sensitive? ascii-fold? stem? stemmer word-delimiter-graph-filter
                 field-name monitor-analyzer monitor-query])

(defn prepare-query-entry
  [query-entry default-type analysis-options]
  (let [with-analysis-options (-> query-entry
                                  (normalize-dictionary-entry default-type)
                                  (inject-analysis-options analysis-keys analysis-options))
        field-name (text-analysis/get-field-name with-analysis-options analysis-options)
        monitor-analyzer (text-analysis/get-string-analyzer with-analysis-options analysis-options)]
    (Dict.
      (:id with-analysis-options)
      (:query with-analysis-options)
      (:type with-analysis-options)
      (:meta with-analysis-options)
      (:tokenizer with-analysis-options)
      (:case-sensitive? with-analysis-options)
      (:ascii-fold? with-analysis-options)
      (:stem? with-analysis-options)
      (:stemmer with-analysis-options)
      (:word-delimiter-graph-filter with-analysis-options)
      field-name
      monitor-analyzer
      (query->monitor-query with-analysis-options field-name monitor-analyzer))))

(defn indexed [questionnaire]
  (loop [index 0
         entries questionnaire
         acc []]
    (if entries
     (recur (inc index)
            (next entries)
            (conj acc (update (nth entries 0) :id #(or % (str index)))))
     acc)))

(defn normalize [questionnaire default-type options]
  (let [analysis-options (merge default-text-analysis (select-keys options analysis-keys))]
    (->> questionnaire
         ; Make sure that each dictionary entry has an :id
         indexed
         ; Add text analysis details
         (r/map (fn [dictionary-entry]
                  (prepare-query-entry dictionary-entry default-type analysis-options)))
         (r/foldcat))))

(defn get-monitor-queries
  "Returns a list MonitorQuery vector"
  [questionnaire]
  (->> questionnaire
       (r/map :monitor-query)
       (r/remove nil?)
       (into [])))
