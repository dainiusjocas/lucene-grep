(ns lmgrep.lucene.field-name-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.lucene.field-name :as ta]))

(deftest field-name-construction
  (testing "empty config"
    (let [analysis-conf {}]
      (is (= "text.standard-tokenizer"
             (ta/construct analysis-conf)))))

  (testing "predefined analyzer"
    (let [analysis-conf {:analyzer {:name "English"}}]
      (is (= "text.English-0"
             (ta/construct analysis-conf)))))

  (testing "only tokenizer is specified"
    (let [analysis-conf {:tokenizer {:name "whitespace"}}]
      (is (= "text.whitespace-0-tokenizer"
             (ta/construct analysis-conf)))))

  (testing "tokenizer is specified with args"
    (let [analysis-conf {:tokenizer {:name "whitespace"
                                     :args {:maxTokenLen 5}}}]
      (is (= "text.whitespace--1255721085-tokenizer"
             (ta/construct analysis-conf)))))

  (testing "char filters are used"
    (let [analysis-conf {:tokenizer {:name "whitespace"
                                     :args {:maxTokenLen 5}}
                         :char-filters [{:name "patternReplace"
                                         :args {:pattern "foo"
                                                :replacement "bar"}}]}]
      (is (= "text.patternReplace-1793830199.whitespace--1255721085-tokenizer"
             (ta/construct analysis-conf)))))

  (testing "token filters are used"
    (let [analysis-conf {:tokenizer {:name "whitespace"
                                     :args {:maxTokenLen 5}}
                         :char-filters [{:name "patternReplace"
                                         :args {:pattern "foo"
                                                :replacement "bar"}}]
                         :token-filters [{:name "uppercase"}]}]
      (is (= "text.patternReplace-1793830199.whitespace--1255721085-tokenizer.uppercase-0"
             (ta/construct analysis-conf))))))
