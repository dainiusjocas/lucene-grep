(ns lmgrep.lucene.query-parser
  (:require [clojure.string :as str]
            [lmgrep.lucene.analyzer :as a])
  (:import (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.search Query BooleanClause$Occur)
           (org.apache.lucene.analysis Analyzer)
           (org.apache.lucene.util QueryBuilder)))

; BasicQueryFactory
(def basic-query-factory-defaults
  {:maxBasicQueries 1024})

; ComplexPhraseQueryParser
(def complex-phrase-query-parser
  {:InOrder true
   })

; QueryParser
(def query-parser-defaults
  {:tAutoGeneratePhraseQueries false
   :SplitOnWhitespace true})

; QueryParserBase
(def query-parser-base-defaults
  {:MaxDeterminizedStates 10000})

; CommonQueryParserConfiguration
(def common-query-parser-configuration-defaults
  {:AllowLeadingWildcard     false
   :EnablePositionIncrements false
   :MultiTermRewriteMethod   "CONSTANT_SCORE_REWRITE"
   :FuzzyPrefixLength        0
   :Locale                   "en"
   :TimeZone                 nil
   :PhraseSlop               0
   :FuzzyMinSim              (float 2)
   :DateResolution nil})

; QueryBuilder
(def query-builder-defaults
  {:enable-position-increments                    true
   :enable-graph-queries                          true
   :autoGenerate-multi-term-synonyms-phrase-query false})

; SimpleQueryParser
(def simple-query-parser-defaults
  {:flags                                         -1
   :default-operator                              "should"})

(defn foo [qb]
  (when (instance? QueryBuilder qb)
    (apply qb query-builder-defaults))
  )

(defmulti configure
          (fn [questionnaire-entry _]
            (keyword (get questionnaire-entry :query-parser))))

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
  (let [query-parser-conf (get questionnaire-entry :query-parser-conf)
        sqp (SimpleQueryParser. monitor-analyzer
                                {field-name (float 1)}
                                (int (or (get query-parser-conf :flags)
                                         (get simple-query-parser-defaults :flags))))]
    (.setDefaultOperator sqp (BooleanClause$Occur/valueOf
                               (str/upper-case
                                 (or (get query-parser-conf :flags)
                                     (get simple-query-parser-defaults :default-operator "should")))))
    (.parse sqp (get questionnaire-entry :query))))

(defn ^Query construct-query [questionnaire-entry ^String field-name ^Analyzer monitor-analyzer]
  (case (keyword (get questionnaire-entry :query-parser))
    :classic (classic-query questionnaire-entry field-name monitor-analyzer)
    :complex-phrase (complex-phrase-query questionnaire-entry field-name monitor-analyzer)
    :surround (surround-query questionnaire-entry field-name monitor-analyzer)
    :simple (simple-query questionnaire-entry field-name monitor-analyzer)
    :standard (standard-query questionnaire-entry field-name monitor-analyzer)
    (classic-query questionnaire-entry field-name monitor-analyzer)))

(comment
  (.clauses (construct-query
     {:query "fox AND \"foo bar\"", :query-parser nil, :id "0", :type "QUERY"}
     "text.standard-0-tokenizer.lowercase-0.asciifolding-0.englishMinimalStem-0"
     (a/create {}))))
