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

(def query-parser-class->attrs
  {SimpleQueryParser
   {:default-operator
    {:default "should"
     :handler (fn [^SimpleQueryParser qp conf]
                (.setDefaultOperator
                  qp (BooleanClause$Occur/valueOf
                       (str/upper-case (get conf :default-operator)))))}}
   QueryParser
   {:auto-generate-phrase-queries
    {:default false
     :handler (fn [^QueryParser qp conf]
                (.setAutoGeneratePhraseQueries qp (get conf :auto-generate-phrase-queries)))}
    :split-on-whitespace
    {:default true
     :handler (fn [^QueryParser qp conf]
                (.setSplitOnWhitespace qp (get conf :split-on-whitespace)))}}
   QueryParserBase
   {:max-determinized-states
    {:default 10000
     :handler (fn [^QueryParserBase qp conf]
                (.setMaxDeterminizedStates qp (int (get conf :max-determinized-states))))}}
   QueryBuilder
   {:enable-position-increments
    {:default true
     :handler (fn [^QueryBuilder qp conf]
                (.setEnablePositionIncrements qp (get conf :enable-position-increments)))}
    :enable-graph-queries
    {:default true
     :handler (fn [^QueryBuilder qp conf]
                (.setEnableGraphQueries qp (get conf :enable-graph-queries)))}
    :auto-generate-multi-term-synonyms-phrase-query
    {:default false
     :handler (fn [^QueryBuilder qp conf]
                (.setAutoGenerateMultiTermSynonymsPhraseQuery
                  qp (get conf :auto-generate-multi-term-synonyms-phrase-query)))}}
   CommonQueryParserConfiguration
   {:allow-leading-wildcard
    {:default false
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setAllowLeadingWildcard qp (get conf :allow-leading-wildcard)))}
    :enable-position-increments
    {:default false
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setEnablePositionIncrements qp (get conf :enable-position-increments)))}
    :multi-term-rewrite-method
    {:default "CONSTANT_SCORE_REWRITE"
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setMultiTermRewriteMethod
                  qp
                  (case (str/upper-case (get conf :multi-term-rewrite-method))
                    "CONSTANT_SCORE_REWRITE" MultiTermQuery/CONSTANT_SCORE_REWRITE
                    "SCORING_BOOLEAN_REWRITE" MultiTermQuery/SCORING_BOOLEAN_REWRITE
                    "CONSTANT_SCORE_BOOLEAN_REWRITE" MultiTermQuery/CONSTANT_SCORE_BOOLEAN_REWRITE)))}
    :fuzzy-prefix-length
    {:default 0
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setFuzzyPrefixLength qp (int (get conf :fuzzy-prefix-length))))}
    :locale
    {:default "en"
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setLocale qp (Locale. ^String (get conf :locale))))}
    :time-zone
    {:default nil
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setTimeZone
                  qp (TimeZone/getTimeZone ^String (get conf :time-zone))))}
    :phrase-slop
    {:default 0
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setPhraseSlop qp (int (get conf :phrase-slop))))}
    :fuzzy-min-sim
    {:default (float 2)
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setFuzzyMinSim qp (float (get conf :fuzzy-min-sim))))}
    :date-resolution
    {:default nil
     :handler (fn [^CommonQueryParserConfiguration qp conf]
                (.setDateResolution
                  qp (DateTools$Resolution/valueOf (get conf :date-resolution))))}}
   ComplexPhraseQueryParser
   {:in-order
    {:default true
     :handler (fn [^ComplexPhraseQueryParser qp conf]
                (.setInOrder qp (get conf :in-order)))}}})

(defn set-conf [qp conf defaults]
  (doseq [[key {:keys [handler]}] defaults]
    (when-not (nil? (get conf key))
      (handler qp conf))))

(defn configure [query-parser conf]
  (doseq [[klazz defaults] query-parser-class->attrs]
    (when (instance? klazz query-parser)
      (set-conf query-parser conf defaults)))
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

(defn create
  "Constructs an Object that can be used for later query parsing.
  https://javadoc.io/doc/org.apache.lucene/lucene-queryparser/latest/index.html"
  [query-parser-name conf ^String field-name ^Analyzer analyzer]
  (case query-parser-name
    :classic (classic conf field-name analyzer)
    :complex-phrase (complex-phrase conf field-name analyzer)
    :surround (surround conf)
    :simple (simple conf field-name analyzer)
    :standard (standard conf analyzer)
    (classic conf field-name analyzer)))
