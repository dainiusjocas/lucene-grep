(ns lmgrep.lucene.query-parser
  (:import (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.search Query)
           (org.apache.lucene.analysis Analyzer)))

(defn configure-query-parser [qp questionnaire-entry]
  (if-let [query-parser-conf (get questionnaire-entry :query-parser-conf)]
    (doto qp
      (.setAllowLeadingWildcard (get query-parser-conf :allow-leading-wildcard true)))
    qp))

(defn ^Query classic-query [questionnaire-entry field-name monitor-analyzer]
  (.parse (configure-query-parser
            (QueryParser. field-name monitor-analyzer)
            questionnaire-entry)
          ^String (get questionnaire-entry :query)))

(defn ^Query complex-phrase-query [questionnaire-entry field-name monitor-analyzer]
  (.parse (configure-query-parser
            (ComplexPhraseQueryParser. field-name monitor-analyzer)
            questionnaire-entry)
          ^String (get questionnaire-entry :query)))

(defn ^Query standard-query [questionnaire-entry field-name monitor-analyzer]
  (.parse (configure-query-parser
            (StandardQueryParser. monitor-analyzer)
            questionnaire-entry)
          ^String (get questionnaire-entry :query) field-name))

(defn ^Query surround-query
  "Monitor analyzer is not applied on the query terms."
  [questionnaire-entry field-name monitor-analyzer]
  (.makeLuceneQueryField (org.apache.lucene.queryparser.surround.parser.QueryParser/parse
                           (get questionnaire-entry :query))
                         field-name (BasicQueryFactory.)))

(defn ^Query simple-query [questionnaire-entry ^String field-name ^Analyzer monitor-analyzer]
  (.parse (SimpleQueryParser. monitor-analyzer field-name)
          (get questionnaire-entry :query)))

(defn ^Query construct-query [questionnaire-entry ^String field-name ^Analyzer monitor-analyzer]
  (case (keyword (get questionnaire-entry :query-parser))
    :classic (classic-query questionnaire-entry field-name monitor-analyzer)
    :complex-phrase (complex-phrase-query questionnaire-entry field-name monitor-analyzer)
    :surround (surround-query questionnaire-entry field-name monitor-analyzer)
    :simple (simple-query questionnaire-entry field-name monitor-analyzer)
    :standard (standard-query questionnaire-entry field-name monitor-analyzer)
    (classic-query questionnaire-entry field-name monitor-analyzer)))
