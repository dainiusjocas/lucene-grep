(ns lmgrep.lucene-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.formatter :as formatter]
            [lmgrep.lucene :as lucene]))

(deftest highlighting-test
  (testing "coloring the output"
    (let [query-string "text"
          highlighter-fn (lucene/highlighter [{:query query-string}])
          text "prefix text suffix"]
      (is (= (str "prefix " \ "[1;31mtext" \ "[0m suffix")
             (formatter/highlight-line text (highlighter-fn text) {}))))))

(deftest highlighter-details
  (testing "simple case"
    (let [text "foo text bar"
          query "text"]
      (is (= [{:query "text" :type "QUERY" :dict-entry-id "0"
               :meta  {"foo" "bar"} :begin-offset 4 :end-offset 8}]
             ((lucene/highlighter [{:query query :id "0" :meta {:foo "bar"}}]) text))))))

(deftest highlighter-with-presearcher
  (testing "presearching implementations should not change the output"
    (let [presearchers #{:no-filtering :term-filtered :multipass-term-filtered}]
      (doseq [presearcher presearchers]
        (let [text "foo text bar"
              query "text"]
          (is (= [{:query "text" :type "QUERY" :dict-entry-id "0"
                   :meta  {"foo" "bar"} :begin-offset 4 :end-offset 8}]
                 ((lucene/highlighter [{:query query :id "0" :meta {:foo "bar"}}]
                                      {:presearcher presearcher}) text))))))))

(deftest word-delimiter-highlights
  (testing "word delimiter"
    (let [text "foo text bar BestClass fooo name"
          query "best class"
          conf {:query    query
                :id       "0"
                :analysis {:tokenizer     {:name "standard"}
                           :token-filters [{:name "worddelimitergraph"
                                            :args {"generateWordParts"   1
                                                   "generateNumberParts" 1
                                                   "preserveOriginal"    1
                                                   "splitOnCaseChange"   1}}
                                           {:name "lowercase"} {:name "asciifolding"}
                                           {:name "englishMinimalStem"}]}}
          dictionary [conf]]
      (is (= [{:begin-offset  13
               :dict-entry-id "0"
               :end-offset    17
               :meta          {}
               :query         "best class"
               :type          "QUERY"}
              {:begin-offset  17
               :dict-entry-id "0"
               :end-offset    22
               :meta          {}
               :query         "best class"
               :type          "QUERY"}]
             ((lucene/highlighter dictionary {}) text))))))
