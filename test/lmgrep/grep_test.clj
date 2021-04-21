(ns lmgrep.grep-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.grep :as grep]))

(deftest grepping-file
  (let [file "test/resources/test.txt"
        query "fox"
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (str/trim
             (with-out-str
               (grep/grep [query] file nil options)))))))

(deftest grepping-stdin
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep [query] nil nil options))))))))


(deftest should-output-no-lines
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog\nfoo"
        query "bar"
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= ""
           (with-in-str text-from-stdin
                        (with-out-str
                          (grep/grep [query] nil nil options)))))))

(deftest grepping-stdin-with-detailed-json-output
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:format :json :with-details true}]
    (is (= {:line-number 1
            :line        text-from-stdin
            :highlights  [{:type          "QUERY"
                           :dict-entry-id "0"
                           :meta          {}
                           :begin-offset  16
                           :end-offset    19
                           :query         query}]}
           (json/read-value
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))
             json/keyword-keys-object-mapper)))))

(deftest grepping-stdin-with-detailed-json-output-with-score
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:format :json :with-details true :with-score true}]
    (is (= {:highlights  [{:dict-entry-id "0"
                           :meta          {}
                           :query         "fox"
                           :score         0.13076457
                           :type          "QUERY"}]
            :line        "The quick brown fox jumps over the lazy dog"
            :line-number 1
            :score       0.13076457}
           (json/read-value
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))
             json/keyword-keys-object-mapper)))))

(deftest grepping-stdin-with-detailed-json-output-with-scored-highlights
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:format :json :with-details true :with-scored-highlights true}]
    (is (= {:highlights  [{:begin-offset  16
                           :dict-entry-id "0"
                           :end-offset    19
                           :meta          {}
                           :query         "fox"
                           :score         0.13076457
                           :type          "QUERY"}]
            :line        "The quick brown fox jumps over the lazy dog"
            :line-number 1
            :score       0.13076457}
           (json/read-value
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))
             json/keyword-keys-object-mapper))))

  (testing "fuzzy matching"
    (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
          query "fxo~2"
          options {:format :json :with-details true :with-scored-highlights true}]
      (is (= {:highlights  [{:begin-offset  16
                             :dict-entry-id "0"
                             :end-offset    19
                             :meta          {}
                             :query         "fxo~2"
                             :score         0.08717638
                             :type          "QUERY"}]
              :line        "The quick brown fox jumps over the lazy dog"
              :line-number 1
              :score       0.08717638}
             (json/read-value
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (grep/grep [query] nil nil options))))
               json/keyword-keys-object-mapper))))))

(deftest grepping-multiple-queries
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        queries ["fox" "dog"]
        options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy >dog<"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep queries nil nil options))))))))

(deftest grepping-multiple-queries-from-file
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        queries []
        options {:split true
                 :pre-tags ">"
                 :post-tags "<"
                 :template "{{highlighted-line}}"
                 :queries-file "test/resources/queries.json"}]
    (is (= "The quick brown >fox< jumps over the lazy >dog<"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep queries nil nil options))))))))

(deftest grepping-multiple-queries-from-file-options
  (testing "options text analysis is injected into dictionary entry if not present"
    (let [text-from-stdin (str/upper-case "The quick brown fox jumps over the lazy dog")
          queries []
          options {:split           true
                   :case-sensitive? true
                   :pre-tags        ">"
                   :post-tags       "<"
                   :template        "{{highlighted-line}}"
                   :queries-file    "test/resources/queries.json"}]
      (is (= ""
            (with-in-str text-from-stdin
                         (str/trim
                           (with-out-str
                             (grep/grep queries nil nil options)))))))))

(defn json-decode [str]
  (if (str/blank? str)
    ""
    (json/read-value str json/keyword-keys-object-mapper)))

(deftest grepping-multiple-queries-from-file-with-meta
  (testing "options text analysis is injected into dictionary entry if not present"
    (let [text-from-stdin (str/upper-case "The quick brown fox jumps over the lazy dog")
          queries []
          options {:split        true
                   :format       :json
                   :with-details true
                   :queries-file "test/resources/queries.json"}]
      (is (= {:highlights  [{:begin-offset  16
                             :dict-entry-id "0"
                             :end-offset    19
                             :meta          {:foo "bar"}
                             :query         "fox"
                             :type          "QUERY"}
                            {:begin-offset  40
                             :dict-entry-id "1"
                             :end-offset    43
                             :meta          {}
                             :query         "dog"
                             :type          "QUERY"}]
              :line        "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG"
              :line-number 1}
             (json-decode
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (grep/grep queries nil nil options))))))))))

(deftest grepping-multiple-queries-from-file-multilingual
  (testing "german stemmer is different than german despite the fact that both dictionary entries are equal"
    (let [text-from-stdin "The quick brown fox jumps over the lazy doggy"
          queries []
          options {:split        true
                   :stemmer      :english
                   :queries-file "test/resources/queries-multilingual.json"
                   :format       :json
                   :with-details true}]
      (is (= {:highlights  [{:begin-offset  40
                             :dict-entry-id "english_language"
                             :end-offset    45
                             :meta          {}
                             :query         "doggies"
                             :type          "QUERY"}]
              :line        "The quick brown fox jumps over the lazy doggy"
              :line-number 1}
             (json-decode
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (grep/grep queries nil nil options))))))))))

(deftest grepping-when-no-match-with-flag-to-println-empty-line
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "foo"
        options {:split true
                 :pre-tags ">"
                 :post-tags "<"
                 :with-empty-lines true
                 :template "{{highlighted-line}}"}]
    (is (= "\n" (with-in-str text-from-stdin
                             (with-out-str
                               (grep/grep [query] nil nil options)))))))

(deftest concurrency-preserves-order-of-input
  (testing "the highlights should be returned in the same order as the input"
    ; Lucene has a bug that fails to find all matches when called concurrently
    ; https://issues.apache.org/jira/browse/LUCENE-9791
    ; therefore the test is not stable
    (let [size 10
          text (str/join "\n" (range size))
          options {:split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
      (is (= (str/join "\n" (map (fn [s] (str ">" s "<")) (range size)))
             (with-in-str text
                          (str/trim
                            (with-out-str
                              (grep/grep (map str (range size)) nil nil options)))))))))

(deftest combination-of-flags-and-analysis-conf
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "jump"
        options {:split true
                 :pre-tags ">"
                 :post-tags "<"
                 :with-empty-lines true
                 :template "{{highlighted-line}}"}]

    (testing "standard analyzer should produce no matches"
      (let [options (assoc options :analysis {:analyzer {:name "standard"}})]
        (is (= "\n" (with-in-str text-from-stdin
                                 (with-out-str
                                   (grep/grep [query] nil nil options)))))))

    (testing "English analyzer should produce a matche"
      (let [options (assoc options :analysis {:analyzer {:name "english"}})]
        (is (= "The quick brown fox >jumps< over the lazy dog\n" (with-in-str text-from-stdin
                                 (with-out-str
                                   (grep/grep [query] nil nil options)))))))))
