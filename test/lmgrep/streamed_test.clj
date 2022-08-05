(ns lmgrep.streamed-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [jsonista.core :as json]
            [lmgrep.streamed :as streamed]))

(deftest streamed-processing
  (testing "Fuzzy match behaviour ordered"
    (let [text-from-stdin "{\"query\": \"nike~\", \"text\": \"I am selling nikee\"}"
          options {:preserve-order true
                   :split          true
                   :pre-tags       ">" :post-tags "<"
                   :template       "{{highlighted-line}}"}]
      (is (= "I am selling >nikee<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (streamed/grep options))))))))

  (testing "Fuzzy match behaviour without preserving the order"
    (let [text-from-stdin "{\"query\": \"nike~\", \"text\": \"I am selling nikee\"}"
          options {:preserve-order false
                   :split          true
                   :pre-tags       ">" :post-tags "<"
                   :template       "{{highlighted-line}}"}]
      (is (= "I am selling >nikee<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (streamed/grep options))))))))

  (testing "Fuzzy match behaviour with score"
    (let [text-from-stdin "{\"query\": \"nike~\", \"text\": \"I am selling nikee\"}"
          options {:preserve-order true
                   :split          true
                   :pre-tags       ">" :post-tags "<"
                   :with-score     true
                   :with-details   true
                   :format         :json}
          expected {"highlights"  [{"dict-entry-id" "597795130"
                                    "meta"          {}
                                    "query"         "nike~"
                                    "score"         0.09807344
                                    "type"          "QUERY"}]
                    "line"        "I am selling nikee"
                    "line-number" 1
                    "score"       0.09807344}]
      (is (= expected
             (json/read-value
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (streamed/grep options)))))))))

  (testing "Fuzzy match behaviour with scored-highlights"
    (let [text-from-stdin "{\"query\": \"nike~\", \"text\": \"I am selling nikee\"}"
          options {:preserve-order         true
                   :split                  true
                   :pre-tags               ">" :post-tags "<"
                   :with-scored-highlights true
                   :with-details           true
                   :format                 :json}
          expected {"highlights"  [{"begin-offset"  13
                                    "dict-entry-id" "597795130"
                                    "end-offset"    18
                                    "meta"          {}
                                    "query"         "nike~"
                                    "score"         0.09807344
                                    "type"          "QUERY"}]
                    "line"        "I am selling nikee"
                    "line-number" 1
                    "score"       0.09807344}]
      (is (= expected
             (json/read-value
               (with-in-str text-from-stdin
                            (str/trim
                              (with-out-str
                                (streamed/grep options)))))))))

  (testing "query parser params are working, AND at the start should throw an exception"
    (let [text-from-stdin "{\"query\": \"AND nike~\", \"text\": \"I am selling nikee\"}"
          options {:preserve-order true
                   :query-parser   "simple"
                   :split          true
                   :pre-tags       ">" :post-tags "<"
                   :template       "{{highlighted-line}}"}]
      (is (= "I am selling >nikee<"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (streamed/grep options))))))))

  (doseq [options [{:preserve-order true
                    :query-parser   "simple"
                    :split          true}

                   {:preserve-order false
                    :query-parser   "simple"
                    :split          true}]]

    (testing "JSON that doesn't contain the text"
      (let [text-from-stdin "{\"query\": \"AND nike~\"}"]
        (is (empty?
              (with-in-str
                text-from-stdin
                (with-out-str
                  (streamed/grep options)))))))


    (testing "JSON that doesn't contain query"
      (let [text-from-stdin "{\"text\": \"I am selling nikee\"}"]
        (is (empty?
              (with-in-str
                text-from-stdin
                (with-out-str
                  (streamed/grep options)))))))

    (testing "JSON that doesn't contain neither query nor text"
      (let [text-from-stdin "{}"]
        (is (empty?
              (with-in-str
                text-from-stdin
                (with-out-str
                  (streamed/grep options)))))))

    (testing "invalid JSON"
      (let [text-from-stdin "{\"query\": \"AND nike~\", \"text\": \"I am selling nikee\""]
        (is (empty?
              (with-in-str
                text-from-stdin
                (with-out-str
                  (streamed/grep options)))))))

    (testing "when query and text are empty strings"
      (let [text-from-stdin "{\"query\": \"\", \"text\": \"\"}"]
        (is (empty?
              (with-in-str
                text-from-stdin
                (with-out-str
                  (streamed/grep options)))))))

    (testing "when query and text are nils"
      (let [text-from-stdin "{\"query\": null, \"text\": null}"]
        (is (empty?
              (with-in-str
                text-from-stdin
                (with-out-str
                  (streamed/grep options)))))))))
