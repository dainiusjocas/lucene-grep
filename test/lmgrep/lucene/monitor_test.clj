(ns lmgrep.lucene.monitor-test
  (:require [clojure.test :refer [deftest testing is]]
            [lmgrep.lucene.monitor :as lucene.monitor]
            [lmgrep.lucene.matching :as lucene.matching]))

(def queries [{:query "fox~"
               :query-parser :classic
               :query-parser-conf {:allow-leading-wildcard true}}])

(def text "quick fux jumps")

(deftest simple-cases
  (testing "one text case"
    (let [{:keys [monitor field-names]} (lucene.monitor/setup queries "default-type" {} {})]
      (is (= [{:begin-offset  6
               :dict-entry-id "1045722129"
               :end-offset    9
               :meta          {}
               :query         "fox~"
               :type          "default-type"}]
             (lucene.matching/match-monitor text monitor field-names {})))))

  (testing "multiple texts input cases with just one text"
    (let [{:keys [monitor field-names]} (lucene.monitor/setup queries "default-type" {} {})
          matches (lucene.matching/match-multi [text] monitor field-names {})]
      (is (= 1 (count matches)))
      (is (= [{:doc-id        0
               :begin-offset  6
               :dict-entry-id "1045722129"
               :end-offset    9
               :meta          {}
               :query         "fox~"
               :type          "default-type"}] matches))))

  (testing "multiple texts input cases with two texts"
    (let [texts ["text fox" "start fux end"]
          {:keys [monitor field-names]} (lucene.monitor/setup queries "default-type" {} {})
          matches (lucene.matching/match-multi texts monitor field-names {})]
      (is (= 2 (count matches)))
      (is (= [{:doc-id        0
               :begin-offset  5
               :dict-entry-id "1045722129"
               :end-offset    8
               :meta          {}
               :query         "fox~"
               :type          "default-type"}
              {:doc-id        1
               :begin-offset  6
               :dict-entry-id "1045722129"
               :end-offset    9
               :meta          {}
               :query         "fox~"
               :type          "default-type"}] matches)))))

(comment
  (testing "foo"
    (let [{:keys [monitor field-names]} (lucene.monitor/setup [{:query "fox"
                                                                :query-parser :classic
                                                                :query-parser-conf {:allow-leading-wildcard true}}] "default-type" {} {})]

      (time (count (reduce (fn [acc _] (conj! acc (lucene.matching/match-monitor "quick fox jumps" monitor field-names {}))) (transient []) (range 0 10000))))
      (time (count (lucene.matching/match-multi (repeat 10000 "quick fox jumps") monitor field-names {}))))))
