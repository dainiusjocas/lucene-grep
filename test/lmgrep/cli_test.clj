(ns lmgrep.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [lmgrep.cli :as cli]))

(def default-options
  {:analysis               {}
   :hyperlink              false
   :only-analyze           false
   :skip-binary-files      false
   :split                  true
   :with-details           false
   :with-empty-lines       false
   :with-scored-highlights false
   :concurrency            8
   :hidden                 true})

(deftest args-handling
  (testing "positional arguments"
    (let [args ["test"]
          {:keys [arguments options error]} (cli/handle-args args)]
      (is (= ["test"] arguments))
      (is (= nil error))
      (is (= default-options options))))

  (testing "positional arguments with queries"
    (let [args ["test" "-q" "foo" "--query=bar"]
          {:keys [arguments options error]} (cli/handle-args args)]
      (is (= ["test"] arguments))
      (is (= nil error))
      (is (= (assoc default-options :query ["bar" "foo"]) options))))

  (testing "positional arguments with queries file"
    (let [args ["test" "-q" "foo" "--queries-file=README.md"]
          {:keys [arguments options error]} (cli/handle-args args)]
      (is (= ["test"] arguments))
      (is (= nil error))
      (is (= (assoc default-options :query ["foo"] :queries-file "README.md") options))))

  (testing "only format"
    (let [args ["--format=edn"]
          {:keys [arguments options error]} (cli/handle-args args)]
      (is (= [] arguments))
      (is (= nil error))
      (is (= (assoc default-options :format :edn) options))))

  (testing "excludes glob"
    (let [args ["--excludes=**.edn"]
          {:keys [arguments options error]} (cli/handle-args args)]
      (is (= [] arguments))
      (is (= nil error))
      (is (= (assoc default-options :excludes "**.edn") options))))

  (testing "with score is a boolean"
    (let [args ["--with-score"]
          {:keys [arguments options error]} (cli/handle-args args)]
      (is (= [] arguments))
      (is (= nil error))
      (is (= (assoc default-options :with-score true) options)))))
