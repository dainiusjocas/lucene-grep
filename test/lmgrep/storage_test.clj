(ns lmgrep.storage-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [babashka.fs :as fs]
            [lmgrep.lucene :as lucene]))

(def dir "target/index-test")

(defn clean-index-files-fixture [f]
  (fs/delete-tree dir)
  (f))

(use-fixtures :each clean-index-files-fixture)

(deftest basic-indexing-into-disk
  (testing "Writing to index on disk and querying writes an index to file then query
  without a query and the output should be the same because the queries are loaded from disk"
    (let [text "foo text bar"
          query {:query "text"}]
      (is (= (lucene/highlight [query] {:queries-index-dir dir} text {})
             (lucene/highlight [] {:queries-index-dir dir} text {}))))))
