(ns lmgrep.lucene.query-parser-test
  (:require [clojure.test :refer :all]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.query-parser :as qp])
  (:import (org.apache.lucene.queryparser.classic QueryParser QueryParser$Operator)
           (org.apache.lucene.queryparser.simple SimpleQueryParser)
           (org.apache.lucene.search BooleanClause BooleanClause$Occur)))

(def field-name "field-name")
(def analyzer (analyzer/create {}))
(def empty-config {})

(deftest simple-query-parser-creation
  (testing "configuration differences"
    (let [config {:flags 123
                  :default-operator "must"}
          default-qp (qp/simple empty-config field-name analyzer)
          qp (qp/simple config field-name analyzer)]
      ; TODO: flag behavior only from the query string parsing output
      (is (instance? SimpleQueryParser default-qp))
      (is (instance? SimpleQueryParser qp))
      (is (= BooleanClause$Occur/SHOULD (.getDefaultOperator default-qp)))
      (is (= BooleanClause$Occur/MUST (.getDefaultOperator qp)))))

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
    (let [config {:max-determinized-states 123}
          default-qp (qp/classic empty-config field-name analyzer)
          qp (qp/classic config field-name analyzer)]
      (is (instance? QueryParser qp))
      (is (instance? QueryParser default-qp))
      (is (= 123 (.getMaxDeterminizedStates qp)))
      (is (= 10000 (.getMaxDeterminizedStates default-qp)))))

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
