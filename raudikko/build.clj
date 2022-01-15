(ns build
  (:require [clojure.tools.build.api :as b]))

(def basis (b/create-basis {:project "deps.edn"}))
(def class-dir "classes")

(defn clean [& _]
  (b/delete {:path class-dir}))

(defn compile-java [_]
  (clean)
  (b/javac {:src-dirs  ["java"]
            :class-dir class-dir
            :basis     basis
            :java-opts ["-source" "11" "-target" "11"]})
  (println"DONE COMPILING LMGREP RAUDIKKO!"))
