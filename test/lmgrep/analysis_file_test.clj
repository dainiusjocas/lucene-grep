(ns lmgrep.analysis-file-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.analysis :as a])
  (:import (org.apache.lucene.analysis Analyzer)))

(deftest read-analyzers-from-file
  (testing "empty config test"
    (is (= {} (a/read-analysis-conf-from-file nil nil))))
  (testing "basic test with a file"
    (let [file "test/resources/analyzers.json"
          analyzers (a/read-analysis-conf-from-file file {})]
      (is (map? analyzers))
      (is (= 1 (count analyzers)))
      (let [[analyzer-name analyzer] (first analyzers)]
        (is (= "some-custom-analyzer" analyzer-name))
        (is (instance? Analyzer analyzer)))))
  (testing "combination with :config-dir option"
    (let [opts {:config-dir "test/resources/"}
          file "analyzers.json"
          analyzers (a/read-analysis-conf-from-file file opts)]
      (is (map? analyzers))
      (is (= 1 (count analyzers)))
      (let [[analyzer-name analyzer] (first analyzers)]
        (is (= "some-custom-analyzer" analyzer-name))
        (is (instance? Analyzer analyzer)))))
  (testing "combination with :config-dir option"
    (let [opts {:config-dir "test/resources/"}
          file "does-not-exists"]
      (is (thrown? Exception (a/read-analysis-conf-from-file file opts))))))

