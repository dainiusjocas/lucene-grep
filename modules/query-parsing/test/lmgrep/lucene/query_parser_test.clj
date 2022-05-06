(ns lmgrep.lucene.query-parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.lucene.query-parser.parsers :as qp])
  (:import (org.apache.lucene.queryparser.classic QueryParser QueryParser$Operator)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.search BooleanClause$Occur)
           (org.apache.lucene.queryparser.flexible.standard StandardQueryParser)
           (org.apache.lucene.queryparser.complexPhrase ComplexPhraseQueryParser)
           (java.lang.reflect Field)
           (org.apache.lucene.queryparser.surround.query BasicQueryFactory)
           (org.apache.lucene.analysis.standard StandardAnalyzer)))

(def field-name "field-name")
(def analyzer (StandardAnalyzer.))
(def empty-config {})

(defn get-private-field-value [obj field-name]
  ;Field f = obj.getClass().getDeclaredField("stuffIWant"); //NoSuchFieldException
  ;f.setAccessible(true);
  ;Hashtable iWantThis = (Hashtable) f.get(obj); //IllegalAccessException
  (let [^Field f (.getDeclaredField (.getClass obj) field-name)]
    (.setAccessible f true)
    (.get f obj)))

(deftest simple-query-parser-creation
  (testing "configuration differences"
    (let [config {:flags 123
                  :default-operator "must"}
          default-qp (qp/simple empty-config field-name analyzer)
          qp (qp/simple config field-name analyzer)]
      (is (instance? SimpleQueryParser default-qp))
      (is (instance? SimpleQueryParser qp))
      (is (= BooleanClause$Occur/SHOULD (.getDefaultOperator default-qp)))
      (is (= BooleanClause$Occur/MUST (.getDefaultOperator qp)))
      (is (= [-1 123] [(get-private-field-value default-qp "flags")
                       (get-private-field-value qp "flags")]))))

  (testing "undefined params"
    (let [config {:flags "foo"
                  :default-operator "must"}]
      (is (thrown? Exception (qp/simple config field-name analyzer))))

    (let [config {:flags 123
                  :default-operator "foo"}]
      (is (thrown? Exception (qp/simple config field-name analyzer))))))

(deftest classic-qp-creation
  (testing "empty configuration"
    (let [config {}
          query-parser (qp/classic config field-name analyzer)]
      (is (instance? QueryParser query-parser))
      (is (= QueryParser$Operator/OR (.getDefaultOperator query-parser)))))

  (testing "query-parser-base config"
    (let [config {:determinize-work-limit 123}
          default-qp (qp/classic empty-config field-name analyzer)
          qp (qp/classic config field-name analyzer)]
      (is (instance? QueryParser qp))
      (is (instance? QueryParser default-qp))
      (is (= 123 (.getDeterminizeWorkLimit qp)))
      (is (= 10000 (.getDeterminizeWorkLimit default-qp)))))

  (testing "query-builder config"
    (let [config {:enable-position-increments                     false
                  :enable-graph-queries                           false
                  :auto-generate-multi-term-synonyms-phrase-query true}
          default-qp (qp/classic empty-config field-name analyzer)
          qp (qp/classic config field-name analyzer)]
      (is (instance? QueryParser default-qp))
      (is (instance? QueryParser qp))
      (is (= true (.getEnablePositionIncrements default-qp)))
      (is (= false (.getEnablePositionIncrements qp)))
      (is (= true (.getEnableGraphQueries default-qp)))
      (is (= false (.getEnableGraphQueries qp)))
      (is (= false (.getAutoGenerateMultiTermSynonymsPhraseQuery default-qp)))
      (is (= true (.getAutoGenerateMultiTermSynonymsPhraseQuery qp))))))

(deftest standard-qp-configuration
  (testing "if params are applied"
    (let [config {:allow-leading-wildcard     false
                  :enable-position-increments false
                  :multi-term-rewrite-method  "CONSTANT_SCORE_REWRITE"
                  :fuzzy-prefix-length        0
                  :locale                     "en"
                  :time-zone                  nil
                  :phrase-slop                0
                  :fuzzy-min-sim              (float 2.1)
                  :date-resolution            nil}
          default-qp (qp/standard empty-config analyzer)
          qp (qp/standard config analyzer)]
      (is (instance? StandardQueryParser default-qp))
      (is (instance? StandardQueryParser qp))

      (is (= 2.0 (.getFuzzyMinSim default-qp)))
      (is (= (float 2.1) (.getFuzzyMinSim qp))))))

(deftest complex-phrase-query-parser
  (testing "if config is applied"
    (let [config {:in-order false}
          default-qp (qp/complex-phrase empty-config field-name analyzer)
          qp (qp/complex-phrase config field-name analyzer)]
      (is (instance? ComplexPhraseQueryParser default-qp))
      (is (instance? ComplexPhraseQueryParser qp))
      (is (not= (get-private-field-value default-qp "inOrder")
                (get-private-field-value qp "inOrder"))))))

(deftest surround-qp-configuration
  (testing "configurability"
    (let [config {:max-basic-queries (long 123)}
          default-qp (qp/surround empty-config)
          qp (qp/surround config)]
      (is (instance? BasicQueryFactory default-qp))
      (is (instance? BasicQueryFactory qp))
      (is (= [1024 123]
             [(.getMaxBasicQueries default-qp) (.getMaxBasicQueries qp)])))))
