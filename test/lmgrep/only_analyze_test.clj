(ns lmgrep.only-analyze-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [lmgrep.only-analyze :as analyze]
            [jsonista.core :as json]))

(deftest only-analysis
  (testing "file input ordered"
    (let [file-path "test/resources/test.txt"
          options {}]
      (is (= 2 (count
                 (str/split-lines
                   (str/trim
                     (with-out-str
                       (analyze/analyze-lines file-path nil options)))))))))
  (testing "multiple lines from stdin ordered"
    (let [text-from-stdin "foo bar \nbaz quux"
          analyzer {}]
      (is (= "[\"foo\",\"bar\"]\n[\"baz\",\"quux\"]\n"
             (with-in-str text-from-stdin
                          (with-out-str
                            (analyze/analyze-lines nil nil analyzer)))))))

  (testing "multiple lines from stdin not ordered"
    (let [text-from-stdin "foo bar \nbaz quux"
          analyzer {:preserve-order false}]
      (is (= #{["foo" "bar"] ["baz" "quux"]}
             (set (map json/read-value (str/split-lines
                                         (with-in-str text-from-stdin
                                                      (with-out-str
                                                        (analyze/analyze-lines nil nil analyzer)))))))))))
