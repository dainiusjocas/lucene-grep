(ns lmgrep.lucene.query-test
  (:require [clojure.test :refer [deftest is]]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.query :as q])
  (:import (org.apache.lucene.search BooleanQuery)))

(def field-name "field-name")
(def analyzer (analyzer/create {}))
(def empty-config {})

(deftest classic-query-parsing
  (let [query "foo bar baz"
        config {:flags            123
                :default-operator "AND"}
        simple-query (q/parse query :classic config field-name analyzer)]
    (is (instance? BooleanQuery simple-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str simple-query)))))

(deftest complex-phrase-query-parsing
  (let [query "foo bar baz"
        config {:flags            123
                :default-operator "AND"}
        simple-query (q/parse query :complex-phrase config field-name analyzer)]
    (is (instance? BooleanQuery simple-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str simple-query)))))

(deftest surround-query-parsing
  (let [query "3N(joh*, peters*)"
        config {:flags            123
                :default-operator "AND"}
        simple-query (q/parse query :complex-phrase config field-name analyzer)]
    (is (instance? BooleanQuery simple-query))
    (is (= "+field-name:3N +(+field-name:joh*, +field-name:peters*)" (str simple-query)))))

(deftest simple-query-parsing
  (let [query "foo bar baz"
        config {:flags            123
                :default-operator "must"}
        simple-query (q/parse query :simple config field-name analyzer)]
    (is (instance? BooleanQuery simple-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str simple-query)))))

(deftest standard-query-parsing
  (let [query "foo bar baz"
        config {:flags            123
                :default-operator "AND"}
        simple-query (q/parse query :standard config field-name analyzer)]
    (is (instance? BooleanQuery simple-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str simple-query)))))
