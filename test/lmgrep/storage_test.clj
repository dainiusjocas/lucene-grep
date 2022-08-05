(ns lmgrep.storage-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [lmgrep.grep :as grep]
            [lmgrep.lucene :as lucene]))

(def dir "target/index-test")

(defn clean-index-files-fixture [f]
  (fs/delete-tree dir)
  (when-not (fs/exists? "target")
    (fs/create-dir "target"))
  (f))

(use-fixtures :each clean-index-files-fixture)

(deftest basic-indexing-into-disk
  (testing "Writing to index on disk and querying writes an index to file then query
  without a query and the output should be the same because the queries are loaded from disk"
    (let [text "foo text bar"
          query {:query "text"}]
      (is (= (lucene/highlight [query] {:queries-index-dir dir} text {})
             (lucene/highlight [] {:queries-index-dir dir} text {}))))))

(deftest preserving-analyzer-per-field
  (testing "if field analyzer is loaded from the file properly"
    (let [text "foo text bar"
          with-default-analysis {:query "texts"}
          query (assoc with-default-analysis :analysis {:tokenizer {:name "whitespace"}})]

      (is (empty? (lucene/highlight [with-default-analysis] {} text {})))

      (is (= (lucene/highlight [query] {:queries-index-dir dir} text {})
             (lucene/highlight [] {:queries-index-dir dir} text {}))))))

(deftest grepping-stdin-unordered
  (testing "Expect that the field text analysis is preserved in the QueriesIndex."
    (let [text-from-stdin "foo texts bar"
          query "text"
          options {:preserve-order    false
                   :split             true
                   :pre-tags          ">" :post-tags "<"
                   :template          "{{highlighted-line}}"
                   :analysis          {:tokenizer     {:name "standard"},
                                       :token-filters [{:name "lowercase"}
                                                       {:name "asciifolding"}
                                                       {:name "englishMinimalStem"}]}
                   :queries-index-dir dir}]
      (is (= "foo >texts< bar"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [query] nil nil options))))))
      (is (= "foo >texts< bar"
             (with-in-str text-from-stdin
                          (str/trim
                            (with-out-str
                              (grep/grep [] nil nil options)))))))))
