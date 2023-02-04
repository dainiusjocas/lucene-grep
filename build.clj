(ns build
  (:require [clojure.tools.deps.cli.api :as c]
            [clojure.tools.build.api :as b]
            [script.env-profiles :as p]))

(defn prep-deps [& _]
  (let [profiles (p/profiles)]
    (println "Preparing transitive dependencies with profiles" profiles)
    (c/prep (assoc (c/basis {:project "deps.edn"
                             :aliases profiles})
              :force true))))

(def class-dir "target/classes")

(defn clean [_]
  (b/delete {:path "target"}))

(defn uberjar [args]
  (let [profiles (p/profiles)
        basis (b/create-basis {:project "deps.edn"
                               :aliases profiles})]
    (println "Building uberjar with profiles" profiles)
    (b/copy-dir {:src-dirs   ["src" "resources"]
                 :target-dir class-dir})
    (b/compile-clj {:basis     basis
                    :src-dirs  ["src"]
                    :class-dir class-dir})
    (b/uber {:class-dir         class-dir
             :uber-file         "target/lmgrep.jar"
             :basis             basis
             :main              'lmgrep.core
             :conflict-handlers {}})))
