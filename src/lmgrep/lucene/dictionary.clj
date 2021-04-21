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

(def analysis-keys #{:case-sensitive?
                     :ascii-fold?
                     :stem?
                     :tokenizer
                     :stemmer
                     :word-delimiter-graph-filter})

(defrecord Dict [field-name monitor-analyzer monitor-query])

(def default-text-analysis
  {:tokenizer nil
   :token-filters [{:name "lowercase"}
                   {:name "asciifolding"}
                   {:name "englishMinimalStem"}]})

(defn override-acm [acm flags]
  (let [tokenizer (or (when-let [tokenizer-kw (get flags :tokenizer)]
                        (text-analysis/tokenizer tokenizer-kw))
                      (:tokenizer acm))
        token-filters (cond->> (get acm :token-filters)
                               (true? (get flags :case-sensitive?))
                               (remove (fn [tf] (= "lowercase" (name (get tf :name)))))
                               (false? (get flags :ascii-fold?))
                               (remove (fn [tf] (= "asciifolding" (name (get tf :name)))))
                               (false? (get flags :stem?))
                               (remove (fn [tf] (re-matches #".*[Ss]tem.*" (name (get tf :name)))))
                               (keyword? (keyword (get flags :stemmer)))
                               ((fn [tfs]
                                  (conj (into [] (remove (fn [tf] (re-matches #".*[Ss]tem.*" (name (get tf :name))))
                                                         tfs))
                                        {:name (text-analysis/stemmer (keyword (get flags :stemmer)))})))
                               (pos-int? (get flags :word-delimiter-graph-filter))
                               (cons {:name "worddelimitergraph"
                                      :args (text-analysis/wdgf->token-filter-args
                                              (get flags :word-delimiter-graph-filter))}))]
    (assoc acm
      :tokenizer tokenizer
      :token-filters token-filters)))

(defn prepare-analysis-configuration [default-text-analysis options]
  (if (empty? (get options :analysis))
    (let [analysis-flags (select-keys options analysis-keys)]
      (if (empty? analysis-flags)
        default-text-analysis
        (override-acm default-text-analysis analysis-flags)))
    (get options :analysis)))

(defn prepare-query-entry
  [questionnaire-entry default-type global-analysis-conf]
  (let [with-analysis-options (update questionnaire-entry :type (fn [type] (if type type default-type)))
        analysis-conf (prepare-analysis-configuration global-analysis-conf questionnaire-entry)
        field-name (text-analysis/get-field-name analysis-conf)
        monitor-analyzer (text-analysis/get-string-analyzer analysis-conf)]
    (Dict.
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
  (let [global-analysis-conf (prepare-analysis-configuration default-text-analysis options)]
    (->> questionnaire
         ; Make sure that each dictionary entry has an :id
         indexed
         ; Add text analysis details
         (r/map (fn [dictionary-entry]
                  (prepare-query-entry dictionary-entry default-type global-analysis-conf)))
         (r/foldcat))))

(defn get-monitor-queries
  "Returns a list MonitorQuery vector"
  [questionnaire]
  (->> questionnaire
       (r/map :monitor-query)
       (r/remove nil?)
       (into [])))
