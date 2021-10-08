(ns lmgrep.lucene.query-parser-test
  (:require [clojure.test :refer :all]
            [lmgrep.lucene.analyzer :as analyzer]
            [lmgrep.lucene.query-parser :as qp]))

(deftest classic-query-constructor
  (let [questionaire-entry {:query "fox AND \"foo bar\""
                            :query-parser nil, :id "0", :type "QUERY"}
        field-name "field-name"
        analyzer(analyzer/create {})
        query-parser (qp/construct-query questionaire-entry
                                         field-name
                                         analyzer)]
    (is (= nil query-parser))))
