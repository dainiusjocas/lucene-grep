(ns lmgrep.lucene.query-test
  (:require [clojure.test :refer [deftest is]]
            [lmgrep.lucene.query :as q])
  (:import (org.apache.lucene.search BooleanQuery)
           (org.apache.lucene.queryparser.surround.query DistanceRewriteQuery)
           (org.apache.lucene.analysis.standard StandardAnalyzer)))

(def field-name "field-name")
(def empty-config {})

(deftest classic-query-parsing
  (let [query "foo bar baz"
        config {:default-operator "AND"}
        parsed-query (q/parse query :classic config field-name)]
    (is (instance? BooleanQuery parsed-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str parsed-query)))))

(deftest complex-phrase-query-parsing
  (let [query "foo bar baz"
        config {:default-operator "AND"}
        parsed-query (q/parse query :complex-phrase config field-name)]
    (is (instance? BooleanQuery parsed-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str parsed-query)))))

(deftest surround-query-parsing
  (let [query "3N(joh*, peters*)"
        config {:default-operator "AND"}
        parsed-query (q/parse query :surround config field-name)]
    (is (instance? DistanceRewriteQuery parsed-query))))

(deftest simple-query-parsing
  (let [query "foo bar baz"
        config {:default-operator "must"}
        parsed-query (q/parse query :simple config field-name)]
    (is (instance? BooleanQuery parsed-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str parsed-query)))))

(deftest standard-query-parsing
  (let [query "foo bar baz"
        config {:default-operator "AND"}
        parsed-query (q/parse query :standard config field-name)]
    (is (instance? BooleanQuery parsed-query))
    (is (= "+field-name:foo +field-name:bar +field-name:baz" (str parsed-query)))))

(deftest minimal-configs
  (let [query "foo bar baz"]
    (is (= "foo bar baz" (str (q/parse query))))
    (is (= "foo bar baz" (str (q/parse query :classic))))
    (is (= "foo bar baz" (str (q/parse query :classic {}))))
    (is (= "f:foo f:bar f:baz" (str (q/parse query :classic {} "f"))))
    (is (= "f:foo f:bar f:baz" (str (q/parse query :classic {} "f" (StandardAnalyzer.)))))))
