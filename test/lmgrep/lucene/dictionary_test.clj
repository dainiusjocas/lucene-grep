(ns lmgrep.lucene.dictionary-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.lucene.dictionary :as dict]))

(deftest merge-default-with-global
  (let [acm dict/default-text-analysis]
    (testing "default flags changes nothing"
      (let [flags {:case-sensitive?             false
                   :ascii-fold?                 true
                   :stem?                       true
                   :tokenizer                   :standard
                   :stemmer                     :english
                   :word-delimiter-graph-filter (+ 1 2 32 64)}]
        (is (= {:token-filters [{:args {"generateNumberParts" 1
                                        "generateWordParts"   1
                                        "preserveOriginal"    1
                                        "splitOnCaseChange"   1}
                                 :name "worddelimitergraph"}
                                {:name "lowercase"}
                                {:name "asciifolding"}
                                {:name "englishMinimalStem"}]
                :tokenizer     {:name "standard"}}
               (dict/override-acm acm flags)))))

    (testing ":tokenizer :standard specifies tokenizer"
      (let [flags {:tokenizer :whitespace}]
        (is (= {:token-filters [{:name "lowercase"}
                                {:name "asciifolding"}
                                {:name "englishMinimalStem"}]
                :tokenizer     {:args {:rule "java"}
                                :name "whitespace"}}
               (dict/override-acm acm flags)))))

    (testing ":case-sensitive? true removes lowercase token filter"
      (let [flags {:case-sensitive? true}]
        (is (= {:token-filters [{:name "asciifolding"}
                                {:name "englishMinimalStem"}]
                :tokenizer     nil}
               (dict/override-acm acm flags)))))

    (testing ":ascii-fold? false removes asciifolding token filter"
      (let [flags {:ascii-fold? false}]
        (is (= {:token-filters [{:name "lowercase"}
                                {:name "englishMinimalStem"}]
                :tokenizer     nil}
               (dict/override-acm acm flags)))))

    (testing ":stem? false removes stemmer token filter"
      (let [flags {:stem? false}]
        (is (= {:token-filters [{:name "lowercase"}
                                {:name "asciifolding"}]
                :tokenizer     nil}
               (dict/override-acm acm flags)))))

    (testing ":stemmer :german replaces the default stemmer token filter"
      (let [flags {:stemmer :german}]
        (is (= {:token-filters [{:name "lowercase"}
                                {:name "asciifolding"}
                                {:name "germanstem"}]
                :tokenizer     nil}
               (dict/override-acm acm flags)))))

    (testing ":word-delimiter-graph-filter 1 adds first token filter"
      (let [flags {:word-delimiter-graph-filter 1}]
        (is (= {:token-filters [{:args {"generateWordParts" 1}
                                 :name "worddelimitergraph"}
                                {:name "lowercase"}
                                {:name "asciifolding"}
                                {:name "englishMinimalStem"}]
                :tokenizer     nil}
               (dict/override-acm acm flags)))))

    (testing ":word-delimiter-graph-filter 1 and german stemmer adds wdgf as first token filter"
      (let [flags {:word-delimiter-graph-filter 1
                   :stemmer                     :german}]
        (is (= {:token-filters [{:args {"generateWordParts" 1}
                                 :name "worddelimitergraph"}
                                {:name "lowercase"}
                                {:name "asciifolding"}
                                {:name "germanstem"}]
                :tokenizer     nil}
               (dict/override-acm acm flags)))))))

(deftest merge-acl-with-flags
  (let [acm {}]
    (testing ":stemmer :german is in token filters list"
      (let [flags {:stemmer :german}]
        (is (= {:token-filters [{:name "germanstem"}]
                :tokenizer     nil}
               (dict/prepare-analysis-configuration acm flags)))))

    (testing "tokenizer from flags ACM with over a flag"
      (let [flags {:analysis  {:tokenizer {:name "whitespace"}}
                   :tokenizer :standard}]
        (is (= {:tokenizer     {:name "whitespace"}}
               (dict/prepare-analysis-configuration acm flags)))))

    (testing "tokenizer from flags ACM with over a flag"
      (let [flags {:analysis  {:token-filters [{:name "englishMinimalStem"}]}
                   :tokenizer :whitespace}]
        (is (= {:token-filters [{:name "englishMinimalStem"}]}
               (dict/prepare-analysis-configuration acm flags)))))

    (testing ":stemmer :english is in the token filters list"
      (let [flags {:analysis {:token-filters [{:name "englishMinimalStem"}]}
                   :stemmer :german}]
        (is (= {:token-filters [{:name "englishMinimalStem"}]}
               (dict/prepare-analysis-configuration acm flags)))))))
