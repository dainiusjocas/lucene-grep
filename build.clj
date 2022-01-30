(ns build
  (:require [clojure.tools.cli.api :as c]
            [clojure.tools.build.api :as b]
            [script.env-profiles :as p]))

(defn prep-deps [& _]
  (println "Preparing transitive dependencies")
  ; TODO: Can it be made to work with multiple aliases in one pass
  (c/prep (assoc (c/basis {:aliases [:raudikko]}) :force true))
  (c/prep (assoc (c/basis {:aliases [:snowball-token-filters]}) :force true)))

(def class-dir "target/classes")

(defn clean [_]
  (b/delete {:path "target"}))

(defn uberjar [args]
  (println "building uberjar with" args)
  (p/profiles)
  (let [basis (b/create-basis {:project "deps.edn"
                               :aliases [:raudikko
                                         :snowball-token-filters
                                         :stempel
                                         :bundled-analyzers]})]
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
