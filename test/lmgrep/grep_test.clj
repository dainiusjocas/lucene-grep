(ns lmgrep.grep-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.grep :as grep]))

(deftest grepping-file
  (let [file "test/resources/test.txt"
        query "fox"
        options {:preserve-order true
                 :split true :pre-tags ">" :post-tags "<" :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (str/trim
             (with-out-str
               (grep/grep [query] file nil options)))))))

(deftest grepping-file-unordered
  (let [file "test/resources/test.txt"
        query "fox"
        options {:preserve-order false
                 :split          true
                 :pre-tags       ">" :post-tags "<"
                 :template       "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (str/trim
             (with-out-str
               (grep/grep [query] file nil options)))))))

(deftest grepping-stdin-unordered
  (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
        query "fox"
        options {:preserve-order false
                 :split true
                 :pre-tags ">" :post-tags "<"
                 :template "{{highlighted-line}}"}]
    (is (= "The quick brown >fox< jumps over the lazy dog"
           (with-in-str text-from-stdin
                        (str/trim
                          (with-out-str
                            (grep/grep [query] nil nil options))))))))

(deftest grepping-ordered-vs-unordered
  (let [file "README.md"
        query "test"
        options {:split          true
                 :pre-tags       ">" :post-tags "<"
                 :template       "{{line-number}}"}
        ordered-options (assoc options :preserve-order true)
        ordered-matched-lines (str/split-lines
                                (str/trim
                                  (with-out-str
                                    (grep/grep [query] file nil ordered-options))))
        unordered-options (assoc options :preserve-order false)
        unordered-matched-lines (str/split-lines
                                  (str/trim
                                    (with-out-str
                                      (grep/grep [query] file nil unordered-options))))]
    (is (= (set ordered-matched-lines) (set unordered-matched-lines)))
    (when (= ordered-matched-lines unordered-matched-lines)
      (println "Usually order is different."))))

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
                           :dict-entry-id "1044772177"
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
    (is (= {:highlights  [{:dict-entry-id "1044772177"
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
                           :dict-entry-id "1044772177"
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
                             :dict-entry-id "1081731735"
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

(deftest global-query-parser-setting
  (testing "with classic query parser fails"
    (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
          queries []
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :queries-file "test/resources/problematic-queries.json"}]
      (is (thrown? Exception
                   (with-in-str text-from-stdin
                                (str/trim
                                  (with-out-str
                                    (grep/grep queries nil nil options))))))))
  (testing "with simple query parser it works"
    (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
          queries []
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :query-parser "simple"
                   :queries-file "test/resources/problematic-queries.json"}]
      (is (= "The quick brown >fox< jumps over the lazy >dog<"
                   (with-in-str text-from-stdin
                                (str/trim
                                  (with-out-str
                                    (grep/grep queries nil nil options)))))))))

(deftest grepping-multiple-queries-from-file-options
  (testing "options text analysis is injected into dictionary entry if not present"
    (let [text-from-stdin (str/upper-case "The quick brown fox jumps over the lazy dog")
          queries []
          options {:split           true
                   :analysis {:token-filters []}
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
    (let [text-from-stdin "The quick brown fox jumps over the lazy dog"
          queries []
          options {:split        true
                   :format       :json
                   :with-details true
                   :analysis {:token-filters [{:name "lowercase"}]}
                   :queries-file "test/resources/queries.json"}]
      (is (= {:highlights  [{:begin-offset  16
                             :dict-entry-id "1372536417"
                             :end-offset    19
                             :meta          {:foo "bar"}
                             :query         "fox"
                             :type          "QUERY"}
                            {:begin-offset  40
                             :dict-entry-id "601069600"
                             :end-offset    43
                             :meta          {}
                             :query         "dog"
                             :type          "QUERY"}]
              :line        "The quick brown fox jumps over the lazy dog"
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

(deftest grepping-queries-from-file-with-analysis
  (testing "analysis specified next to each query in a file"
    (let [text-from-stdin "a white dog and a black cat"
          queries []
          options {:split        true
                   :queries-file "test/resources/queries-with-analysis.json"
                   :format       :json
                   :with-details true}]
      (is (= {:highlights  [{:begin-offset  8
                             :dict-entry-id "0"
                             :end-offset    11
                             :meta          {}
                             :query         "dogs"
                             :type          "QUERY"}]
              :line        "a white dog and a black cat"
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
    (let [size 50
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

    (testing "English analyzer should produce a match"
      (let [options (assoc options :analysis {:token-filters [{:name "englishminimalstem"}]})]
        (is (= "The quick brown fox >jumps< over the lazy dog\n"
               (with-in-str text-from-stdin
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))))

(deftest hyperlink-with-stdin
  (let [text-from-stdin "this will generate an exception"
        query "exception"
        options {:split true
                 :hyperlink true}]
    (testing "hyperlinking on stdin should be ignored"
      (let [options (assoc options :analysis {:analyzer {:name "standard"}})]
        (is (= "\u001B[0;35m*STDIN*\u001B[0m:\u001B[0;32m1\u001B[0m:this will generate an \u001B[1;31mexception\u001B[0m\n"
               (with-in-str text-from-stdin
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))))

(deftest grepping-with-simple-query-parser
  (let [text-from-stdin "john foo peters post"
        query "\"john peters\"~2"
        options {:split true
                 :query-parser "simple"
                 :pre-tags ">"
                 :post-tags "<"
                 :template "{{highlighted-line}}"}]
    (testing "simple case"
      (is (= ">john foo peters< post"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))))

(deftest grepping-with-standard-query-parser
  (let [text-from-stdin "john foo peters post"
        query "\"john peters\"~2"
        options {:split true
                 :query-parser "standard"
                 :pre-tags ">"
                 :post-tags "<"
                 :template "{{highlighted-line}}"}]
    (testing "simple case"
      (is (= ">john foo peters< post"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))))

(deftest grepping-with-complex-phrase-query-parser
  (let [text-from-stdin "john foo peters post"
        query "\"john peters\"~2"
        options {:split true
                 :pre-tags ">"
                 :post-tags "<"
                 :template "{{highlighted-line}}"}]
    (testing "without complex phrase flag doesn't match"
      (is (= ">john foo peters< post"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))

    (testing "with complex phrase query parser matches"
      (let [options (assoc options :query-parser "complex-phrase")]
        (is (= ">john< foo >peters< post"
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (grep/grep [query] nil nil options))))))))

    (testing "fuzzy phrase with complex-phrase"
      (let [text-from-stdin "jonathann peterson post"
            query "\"(john jon jonathan~) peters*\""
            options (assoc options :query-parser "complex-phrase")]
        (is (= ">jonathann< >peterson< post"
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (grep/grep [query] nil nil options))))))))))

(deftest grepping-with-the-surround-query-parser
  (testing "basic query"
    (let [text-from-stdin "nike and adidas"
          query "2W(nike, adidas)"
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :analysis {:token-filters [{:name "lowercase"} {:name "asciifolding"}]}
                   :query-parser "surround"}]
      (is (= ">nike< and >adidas<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))))

      (testing "too large distance"
        (is (empty?
              (with-in-str "nike and some adidas"
                           (str/trim
                             (with-out-str
                               (grep/grep [query] nil nil options)))))))

      (testing "out of order"
        (is (empty?
              (with-in-str "adidas and nike"
                           (str/trim
                             (with-out-str
                               (grep/grep [query] nil nil options)))))))))

  (testing "unordered"
    (let [text-from-stdin "adidas and nike"
          query "2N(nike, adidas)"
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :analysis {:token-filters [{:name "lowercase"} {:name "asciifolding"}]}
                   :query-parser "surround"}]
      (is (= ">adidas< and >nike<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))))))

  (testing "fuzzy match"
    (let [text-from-stdin "adidas and nikon"
          query "2N(nik*, adidas)"
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :analysis {:token-filters [{:name "lowercase"} {:name "asciifolding"}]}
                   :query-parser "surround"}]
      (is (= ">adidas< and >nikon<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))
    (let [text-from-stdin "adibas and nikon"
          query "2N(nik*, adi?as)"
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :analysis {:token-filters [{:name "lowercase"} {:name "asciifolding"}]}
                   :query-parser "surround"}]
      (is (= ">adibas< and >nikon<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))))))

  (testing "quoting"
    (let [text-from-stdin "adidas foos"
          query "OR(\"adidas foo\"*, nike)"                 ; `adidas foo` should be one term
          options {:split true
                   :pre-tags ">"
                   :post-tags "<"
                   :template "{{highlighted-line}}"
                   :stem? false
                   :analysis {:tokenizer {:name "keyword"}
                              :token-filters [{:name "lowercase"} {:name "asciifolding"}]}
                   :query-parser "surround"}]
      (is (= ">adidas foos<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options)))))))))

(deftest grepping-multiple-queries-from-file-with-query-parsers
  (let [text-from-stdin "john foo peterson post"
        queries []
        options {:split true
                 :format       :json
                 :with-details true
                 :queries-file "test/resources/queries-query-parsers.json"}]
    (is (= {:highlights  [{:begin-offset  0
                           :dict-entry-id "930149941"
                           :end-offset    4
                           :meta          {}
                           :query         "3N(joh*, peters*)"
                           :type          "QUERY"}
                          {:begin-offset  9
                           :dict-entry-id "930149941"
                           :end-offset    17
                           :meta          {}
                           :query         "3N(joh*, peters*)"
                           :type          "QUERY"}]
            :line        "john foo peterson post"
            :line-number 1}
           (json/read-value
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep queries nil nil options))))
             json/keyword-keys-object-mapper)))))

(deftest allowing-leading-wildcards-for-classic-query-parser
  (testing "by default query parser allows loading wildcards"
    (let [file "test/resources/test.txt"
          query "*fox"
          options {:split true :pre-tags ">" :post-tags "<"
                   :template "{{highlighted-line}}"}]
      (is (= "The quick brown >fox< jumps over the lazy dog"
             (str/trim
               (with-out-str
                 (grep/grep [query] file nil options)))))))

  (testing "explicitly declared :allow-leading-wildcard false causes exception"
    (let [file "test/resources/test.txt"
          query "*fox"
          options {:split true
                   :pre-tags ">" :post-tags "<"
                   :template "{{highlighted-line}}"
                   :query-parser-conf {:allow-leading-wildcard false}}]
      (is (thrown? Exception (grep/grep [query] file nil options)))))

  (testing "explicitly declared :allow-leading-wildcard false causes exception in queries file"
    (let [file "test/resources/test.txt"
          options {:split true
                   :pre-tags ">" :post-tags "<"
                   :template "{{highlighted-line}}"
                   :queries-file "test/resources/query-parser-conf.json"}]
      (is (thrown? Exception (grep/grep [] file nil options))))))
