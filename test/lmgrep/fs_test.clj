(ns lmgrep.fs-test
  (:require [clojure.test :refer [deftest is]]
            [lmgrep.fs :as fs]))

(deftest filter-for-files
  (let [files ["src" "deps.edn"]]
    (is (= ["deps.edn"] (into [] (fs/filter-files files))))))

(deftest excludes-files
  (is (> (count (fs/get-files "test/**.*" {}))
         (count (fs/get-files "test/**.*" {:excludes "**.xml"})))))

(deftest skipping-binary-files
  (is (> (count (fs/get-files "test/**.*" {}))
         (count (fs/get-files "test/**.*" {:skip-binary-files true})))))

(deftest binary-file-shipping
  (when (contains? #{"Linux" "Mac OS X"} (System/getProperty "os.name"))
    ; NOTE: Windows doesn't support this feature
    ; All .png files in test dir should be binary
    (is (= [] (fs/get-files "test/**.png" {:skip-binary-files true})))))

(deftest infering-root-dir
  (is (= "." (fs/infer-root-folder ".env")))
  (is (= "." (fs/infer-root-folder "**.env")))
  (is (= "/home/foo" (fs/infer-root-folder "/home/foo/.env")))
  (is (= "/home/foo" (fs/infer-root-folder "/home/foo/**/.env")))

  (is (= "/home/foo" (fs/infer-root-folder "/home/foo/**/node_modules/**/.env"))))
