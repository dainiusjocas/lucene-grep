(ns lmgrep.only-analyze-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.only-analyze :as analyze]))

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
          options {}]
      (is (= "[\"foo\",\"bar\"]\n[\"baz\",\"quux\"]\n"
             (with-in-str text-from-stdin
                          (with-out-str
                            (analyze/analyze-lines nil nil options)))))))

  (testing "multiple lines from stdin not ordered"
    (let [text-from-stdin "foo bar \nbaz quux"
          options {:preserve-order false}]
      (is (= #{["foo" "bar"] ["baz" "quux"]}
             (set (map json/read-value (str/split-lines
                                         (with-in-str text-from-stdin
                                                      (with-out-str
                                                        (analyze/analyze-lines nil nil options))))))))))

  (testing "ordered and unordered analysis should produce same set of tokens"
    (let [file-path "test/resources/test.txt"
          ordered-options {:preserve-order true}
          unordered-options {:preserve-order false}
          ordered-tokens (mapcat json/read-value
                                 (str/split-lines
                                   (with-out-str
                                     (analyze/analyze-lines file-path nil ordered-options))))
          unordered-tokens (mapcat json/read-value
                                   (str/split-lines
                                     (with-out-str
                                       (analyze/analyze-lines file-path nil unordered-options))))]
      (is (= (set ordered-tokens) (set unordered-tokens)))
      (is (= ordered-tokens unordered-tokens)))))
