(ns build
  (:require [clojure.tools.build.api :as b]))

(def basis (b/create-basis {:project "deps.edn"}))
(def class-dir "target/classes")
(def java-src-dir "java")
(def lib 'lmgrep/raudikko)
(def version "LATEST")

(defn clean [& _]
  (b/delete {:path class-dir}))

(defn compile-java [_]
  (clean)
  (b/javac {:src-dirs  [java-src-dir]
            :class-dir class-dir
            :basis     basis
            :java-opts ["-source" "11" "-target" "11"]})
  (println"DONE COMPILING LMGREP RAUDIKKO JAVA SOURCES!"))

(defn jar [_]
  (clean)
  (compile-java nil)
  (b/copy-dir {:src-dirs   ["resources"]
               :target-dir class-dir})
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs [java-src-dir]})
  (b/jar {:class-dir         class-dir
          :jar-file         "target/raudikko.jar"})
  (println "DONE JARING RADUKKO LIB!"))
