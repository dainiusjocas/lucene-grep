(ns lmgrep.lucene.query-parser.conf
  (:require [clojure.string :as str])
  (:import (java.util Locale TimeZone)
           (org.apache.lucene.document DateTools$Resolution)
           (org.apache.lucene.queryparser.classic QueryParser QueryParserBase QueryParser$Operator)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser CommonQueryParserConfiguration)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.queryparser.flexible.standard.config StandardQueryConfigHandler$Operator)
           (org.apache.lucene.search BooleanClause$Occur MultiTermQuery)
           (org.apache.lucene.util QueryBuilder)))

(def query-parser-class->attrs
  {StandardQueryParser
   {:default-operator
    {:default "OR"
     :handler (fn [^StandardQueryParser qp conf]
                (.setDefaultOperator
                  qp (StandardQueryConfigHandler$Operator/valueOf
                       (str/upper-case (get conf :default-operator)))))}}
   SimpleQueryParser
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
   {:determinize-work-limit
    {:default 10000
     :handler (fn [^QueryParserBase qp conf]
                (.setDeterminizeWorkLimit qp (int (get conf :determinize-work-limit))))}
    :default-operator
    {:default "OR"
     :handler (fn [^QueryParserBase qp conf]
                (.setDefaultOperator
                  qp
                  (QueryParser$Operator/valueOf
                    ^String (str/upper-case (get conf :default-operator)))))}}
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
