(ns lmgrep.analysis-file-test
  (:require [clojure.test :refer :all]
            [lmgrep.analysis :as a])
  (:import (org.apache.lucene.analysis Analyzer)))

(deftest read-analyzers-from-file
  (let [file "test/resources/analyzers.json"
        analyzers (a/read-analysis-conf-from-file file {})]
    (is (map? analyzers))
    (is (= 1 (count analyzers)))
    (let [[analyzer-name analyzer] (first analyzers)]
      (is (= "some-custom-analyzer" analyzer-name))
      (is (instance? Analyzer analyzer)))))

(deftest read-analyzers-with-config-dir
  (let [opts {:config-dir "test/resources/"}
        file "analyzers.json"
        analyzers (a/read-analysis-conf-from-file file opts)]
    (is (map? analyzers))
    (is (= 1 (count analyzers)))
    (let [[analyzer-name analyzer] (first analyzers)]
      (is (= "some-custom-analyzer" analyzer-name))
      (is (instance? Analyzer analyzer)))))
