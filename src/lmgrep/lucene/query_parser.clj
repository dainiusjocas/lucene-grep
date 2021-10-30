(ns lmgrep.lucene.query-parser
  (:require [clojure.string :as str])
  (:import (org.apache.lucene.queryparser.classic QueryParser QueryParserBase)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser CommonQueryParserConfiguration)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.search Query BooleanClause$Occur MultiTermQuery MultiTermQuery$RewriteMethod)
           (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.util QueryBuilder)
           (java.util Locale TimeZone)
           (org.apache.lucene.document DateTools$Resolution)))

(set! *warn-on-reflection* true)

; BasicQueryFactory
(def basic-query-factory-defaults
  {:maxBasicQueries 1024})

; ComplexPhraseQueryParser
(def complex-phrase-query-parser
  {:in-order true})

; + QueryParser
(def query-parser-defaults
  {:auto-generate-phrase-queries false
   :split-on-whitespace true})

; + QueryParserBase
(def query-parser-base-defaults
  {:max-determinized-states 10000})

; + QueryBuilder
(def query-builder-defaults
  {:enable-position-increments                     true
   :enable-graph-queries                           true
   :auto-generate-multi-term-synonyms-phrase-query false})

; - SimpleQueryParser
(def simple-query-parser-defaults
  {:flags            -1
   :default-operator "should"})

; - CommonQueryParserConfiguration
(def common-query-parser-configuration-defaults
  {:allow-leading-wildcard     false
   :enable-position-increments false
   :multi-term-rewrite-method  "CONSTANT_SCORE_REWRITE"
   :fuzzy-prefix-length        0
   :locale                     "en"
   :time-zone                  nil
   :phrase-slop                0
   :fuzzy-min-sim              (float 2)
   :date-resolution            nil})

(defn with-default [kw conf defaults]
  (let [conf-val (get conf kw)]
    (if (nil? conf-val)
      (get defaults kw)
      conf-val)))

(defn configure-query-parser [qp conf]

  (comment
    "Order is important"
    "only apply when value is provided"
    "meaning the defaults should not be set!!!")

  (when (instance? SimpleQueryParser qp)
    (let [^SimpleQueryParser qp qp]
      (doto qp
        (cond->
          (not (nil? (get conf :default-operator)))
          (.setDefaultOperator (BooleanClause$Occur/valueOf
                                 (str/upper-case
                                   (with-default :default-operator conf simple-query-parser-defaults))))))))

  (when (instance? QueryParser qp)
    (let [^QueryParser qp qp]
      (doto qp
        (cond->
          (not (nil? (get conf :split-on-whitespace)))
          (.setSplitOnWhitespace
            (with-default :split-on-whitespace conf query-parser-defaults)))
        (cond->
          (not (nil? (get conf :auto-generate-phrase-queries)))
          (.setAutoGeneratePhraseQueries
            (with-default :auto-generate-phrase-queries conf query-parser-defaults))))))

  (when (instance? QueryParserBase qp)
    (let [^QueryParserBase qp qp]
      (doto qp
        (cond->
          (not (nil? (get conf :max-determinized-states)))
          (.setMaxDeterminizedStates
            (int
              (with-default :max-determinized-states conf query-parser-base-defaults)))))))

  (when (instance? QueryBuilder qp)
    (let [^QueryBuilder qp qp]
      (doto qp
        (cond->
          (not (nil? (get conf :enable-position-increments)))
          (.setEnablePositionIncrements
            (with-default :enable-position-increments conf query-builder-defaults)))
        (cond->
          (not (nil? (get conf :enable-graph-queries)))
          (.setEnableGraphQueries
            (with-default :enable-graph-queries conf query-builder-defaults)))
        (cond->
          (not (nil? (get conf :auto-generate-multi-term-synonyms-phrase-query)))
          (.setAutoGenerateMultiTermSynonymsPhraseQuery
            (with-default :auto-generate-multi-term-synonyms-phrase-query conf query-builder-defaults))))))

  (when (instance? CommonQueryParserConfiguration qp)
    (let [^CommonQueryParserConfiguration qp qp]
      (doto qp
        (cond->
          (not (nil? (get conf :allow-leading-wildcard)))
          (.setAllowLeadingWildcard
            (with-default :allow-leading-wildcard conf common-query-parser-configuration-defaults)))
        (cond->
          (not (nil? (get conf :enable-position-increments)))
          (.setEnablePositionIncrements
            (with-default :enable-position-increments conf common-query-parser-configuration-defaults)))
        (cond->
          (not (nil? (get conf :multi-term-rewrite-method)))
          ; TODO: proper resolution
          (.setMultiTermRewriteMethod MultiTermQuery/CONSTANT_SCORE_REWRITE
                                      #_(with-default :multi-term-rewrite-method conf common-query-parser-configuration-defaults)))
        (cond->
          (not (nil? (get conf :fuzzy-prefix-length)))
          (.setFuzzyPrefixLength
            (int (with-default :fuzzy-prefix-length conf common-query-parser-configuration-defaults))))
        (cond->
          (not (nil? (get conf :locale)))
          (.setLocale
            (Locale. (with-default :locale conf common-query-parser-configuration-defaults))))
        (cond->
          (not (nil? (get conf :time-zone)))
          (.setTimeZone
            (TimeZone/getTimeZone ^String (with-default :time-zone conf common-query-parser-configuration-defaults))))
        (cond->
          (not (nil? (get conf :phrase-slop)))
          (.setPhraseSlop
            (int (with-default :phrase-slop conf common-query-parser-configuration-defaults))))
        (cond->
          (not (nil? (get conf :fuzzy-min-sim)))
          (.setFuzzyMinSim
            (float (with-default :fuzzy-min-sim conf common-query-parser-configuration-defaults))))
        (cond->
          (not (nil? (get conf :date-resolution)))
          (.setDateResolution
            (DateTools$Resolution/valueOf (with-default :date-resolution conf common-query-parser-configuration-defaults)))))))

  (when (instance? ComplexPhraseQueryParser qp)
    (let [^ComplexPhraseQueryParser qp qp]
      (doto qp
        (cond->
          (not (nil? (get conf :in-order)))
          (.setInOrder (with-default :in-order conf complex-phrase-query-parser))))))

  qp)

;;;;;;;;;; These functions invokes the constructor and then passed it
;;;;;;;;;; to the configuration function

(defn classic-qp [query-parser-conf field-name analyzer]
  (configure-query-parser
    (QueryParser. field-name analyzer)
    query-parser-conf))

(defn complex-phrase-qp [query-parser-conf field-name analyzer]
  (configure-query-parser
    (ComplexPhraseQueryParser. field-name analyzer)
    query-parser-conf))

(defn standard-qp [query-parser-conf _ analyzer]
  (configure-query-parser
    (StandardQueryParser. analyzer)
    query-parser-conf))

(defn ^SimpleQueryParser simple-qp [conf ^String field-name ^Analyzer analyzer]
  (let [sqp (SimpleQueryParser. analyzer
                                {field-name (float 1)}
                                (int (or (get conf :flags)
                                         (get simple-query-parser-defaults :flags))))]
    (configure-query-parser sqp conf)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^Query complex-phrase-query [query-string query-parser-conf ^String field-name analyzer]
  (let [^ComplexPhraseQueryParser qp (complex-phrase-qp query-parser-conf field-name analyzer)]
    (.parse qp ^String query-string)))

(defn ^Query standard-query [query-string query-parser-conf ^String field-name analyzer]
  (let [^StandardQueryParser qp (standard-qp query-parser-conf field-name analyzer)]
    (.parse qp ^String query-string field-name)))

(defn ^Query surround-query
  "Monitor analyzer is not applied on the query terms."
  [query-string query-parser-conf field-name analyzer]
  (.makeLuceneQueryField (org.apache.lucene.queryparser.surround.parser.QueryParser/parse
                           query-string)
                         field-name (BasicQueryFactory.)))

(defn ^Query classic-query [query-string query-parser-conf field-name analyzer]
  (let [^QueryParser qp (classic-qp query-parser-conf field-name analyzer)]
    (.parse qp ^String query-string)))

(defn ^Query simple-query [query-string query-parser-conf ^String field-name ^Analyzer analyzer]
  (let [sqp (simple-qp query-parser-conf ^String field-name ^Analyzer analyzer)]
    (.parse sqp query-string)))

(defn ^Query construct-query [questionnaire-entry ^String field-name ^Analyzer analyzer]
  (let [^String query-string (get questionnaire-entry :query)
        query-parser-name (keyword (get questionnaire-entry :query-parser))
        query-parser-conf (get questionnaire-entry :query-parser-conf)]
    (case query-parser-name
      :classic (classic-query query-string query-parser-conf field-name analyzer)
      :complex-phrase (complex-phrase-query query-string query-parser-conf field-name analyzer)
      :surround (surround-query query-string query-parser-conf field-name analyzer)
      :simple (simple-query query-string query-parser-conf field-name analyzer)
      :standard (standard-query query-string query-parser-conf field-name analyzer)
      (classic-query query-string query-parser-conf field-name analyzer))))
