(ns lmgrep.lucene.custom-analyzer-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.lucene.custom-analyzer :as analyzer]))

(deftest custom-analyzer-validation
  (testing "tokenizer config"
    (let [tokenizer-config-as-string "FAIL"]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:tokenizer tokenizer-config-as-string}))))

    (let [arrays-are-not-allowed []]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:tokenizer arrays-are-not-allowed})))))

  (testing "char filter config should be sequable"
    (let [hashmaps-are-not-allowed {}]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:char-filters hashmaps-are-not-allowed}))))

    (let [strings-are-not-allowed "foo"]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:char-filters strings-are-not-allowed})))))

  (testing "individual char-filter configuration"
    (let [char-filter-config-as-string "FAIL"]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:char-filters [char-filter-config-as-string]}))))

    (let [vector-filter-config-as-string ["FAIL"]]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:char-filters [vector-filter-config-as-string]})))))

  (testing "token filter config should be sequable"
    (let [hashmaps-are-not-allowed {}]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:token-filters hashmaps-are-not-allowed}))))

    (let [strings-are-not-allowed "foo"]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:token-filters strings-are-not-allowed})))))

  (testing "individual token-filter configuration"
    (let [token-filter-config-as-string "FAIL"]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:token-filters [token-filter-config-as-string]}))))

    (let [vector-token-filter-config ["FAIL"]]
      (is (thrown? AssertionError
                   (analyzer/create
                     {:token-filters [vector-token-filter-config]})))))

  (testing "invalid arguments passed throws an Exception"
    (is (thrown? IllegalArgumentException
                 (analyzer/create
                   {:tokenizer {:name "standard"
                                :args {:foo "foo"}}})))))
