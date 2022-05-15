(ns lmgrep.formatter-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.formatter :as f]))

(deftest no-split-formatting
  (testing "multiline handling"
    (let [input-text (slurp "test/resources/test.txt")
          highlights [{:query "dog"
                       :type  "QUERY",
                       :dict-entry-id 601069600,
                       :meta {},
                       :begin-offset 40,
                       :end-offset 43}]
          options {}
          cutout-highlights (f/cutout-highlight input-text highlights options)]
      (is (= "jumps over the lazy \u001B[1;31mdog\u001B[0m Apache Lucene™ is a"
             cutout-highlights)))))
