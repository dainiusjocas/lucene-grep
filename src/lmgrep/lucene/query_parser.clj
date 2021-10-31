(ns lmgrep.lucene.query-parser
  (:require [clojure.string :as str])
  (:import (org.apache.lucene.queryparser.classic QueryParser QueryParserBase)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser CommonQueryParserConfiguration)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.search BooleanClause$Occur MultiTermQuery)
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

(defn configure [query-parser conf]
  (when (instance? SimpleQueryParser query-parser)
    (let [^SimpleQueryParser qp query-parser]
      (doto qp
        (cond->
          (not (nil? (get conf :default-operator)))
          (.setDefaultOperator (BooleanClause$Occur/valueOf
                                 (str/upper-case
                                   (with-default :default-operator conf {}))))))))

  (when (instance? QueryParser query-parser)
    (let [^QueryParser qp query-parser]
      (doto qp
        (cond->
          (not (nil? (get conf :split-on-whitespace)))
          (.setSplitOnWhitespace
            (with-default :split-on-whitespace conf query-parser-defaults)))
        (cond->
          (not (nil? (get conf :auto-generate-phrase-queries)))
          (.setAutoGeneratePhraseQueries
            (with-default :auto-generate-phrase-queries conf query-parser-defaults))))))

  (when (instance? QueryParserBase query-parser)
    (let [^QueryParserBase qp query-parser]
      (doto qp
        (cond->
          (not (nil? (get conf :max-determinized-states)))
          (.setMaxDeterminizedStates
            (int
              (with-default :max-determinized-states conf query-parser-base-defaults)))))))

  (when (instance? QueryBuilder query-parser)
    (let [^QueryBuilder qp query-parser]
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

  (when (instance? CommonQueryParserConfiguration query-parser)
    (let [^CommonQueryParserConfiguration qp query-parser]
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

  (when (instance? ComplexPhraseQueryParser query-parser)
    (let [^ComplexPhraseQueryParser qp query-parser]
      (doto qp
        (cond->
          (not (nil? (get conf :in-order)))
          (.setInOrder (with-default :in-order conf complex-phrase-query-parser))))))

  query-parser)

(defn classic [conf field-name analyzer]
  (configure (QueryParser. field-name analyzer) conf))

(defn complex-phrase [conf field-name analyzer]
  (configure (ComplexPhraseQueryParser. field-name analyzer) conf))

(defn standard [conf analyzer]
  (configure (StandardQueryParser. analyzer) conf))

(defn ^SimpleQueryParser simple [conf ^String field-name ^Analyzer analyzer]
  (let [flags (int (get conf :flags -1))
        sqp (SimpleQueryParser. analyzer {field-name (float 1)} flags)]
    (configure sqp conf)))

(defn surround [conf]
  (let [max-basic-queries (int (get conf :max-basic-queries 1024))]
    (BasicQueryFactory. max-basic-queries)))

(defn create [query-parser-name conf ^String field-name ^Analyzer analyzer]
  (case query-parser-name
    :classic (classic conf field-name analyzer)
    :complex-phrase (complex-phrase conf field-name analyzer)
    :surround (surround conf)
    :simple (simple conf field-name analyzer)
    :standard (standard conf analyzer)
    (classic conf field-name analyzer)))
