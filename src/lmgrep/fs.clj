(ns lmgrep.fs
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.nio.file FileSystems PathMatcher Path)
           (java.io File)))

; TODO: Support regex pattern
(defn get-files [^String glob]
  (let [glob-file (io/file glob)
        pathname-parent (or (.getParent glob-file) ".")
        starting-folder (if (= "**" pathname-parent)
                          "."
                          (str/replace pathname-parent #"\*\*/?" ""))
        ^PathMatcher grammar-matcher (.getPathMatcher
                                       (FileSystems/getDefault)
                                       (str "glob:" glob))]
    (->> starting-folder
         io/file
         file-seq
         (filter (fn [^File f] (.isFile f)))
         (filter (fn [^File f]
                   (if (or (.getParent glob-file) (re-find #"\*\*" glob))
                     (.matches grammar-matcher ^Path (.toPath ^File f))
                     (when (= starting-folder (str (.getParent (.toPath ^File f))))
                       (.matches grammar-matcher ^Path (.getFileName (.toPath ^File f)))))))
         (mapv #(.getPath ^File %)))))

(comment
  (lmgrep.fs/get-files "*.md")
  (lmgrep.fs/get-files "*.clj")
  (lmgrep.fs/get-files "./src/lmgrep/*.clj")
  (lmgrep.fs/get-files "**.clj")
  (lmgrep.fs/get-files "**/*.clj")
  (lmgrep.fs/get-files "classes/**.class")
  (lmgrep.fs/get-files "/var/log/**.log"))
