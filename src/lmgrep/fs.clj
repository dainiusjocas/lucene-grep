(ns lmgrep.fs
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str])
  (:import (java.nio.file FileSystems PathMatcher Path)
           (java.io File)))

(def file-options (case (System/getProperty "os.name")
                    "Linux" "-ib"
                    "Mac OS X" "-I"
                    nil))

(defn binary-file? [^String file-path]
  (.contains ^String (:out (sh/sh "file" file-options file-path))
             "charset=binary"))

(defn remove-binary-files [file-paths options]
  (if (and (:skip-binary-files options) file-options)
    (remove binary-file? file-paths)
    file-paths))

; TODO: Support regex pattern
(defn get-files [^String glob options]
  (let [glob-file (io/file glob)
        pathname-parent (or (.getParent glob-file) ".")
        starting-folder (if (= "**" pathname-parent)
                          "."
                          (str/replace pathname-parent #"\*\*/?" ""))
        ^PathMatcher grammar-matcher (.getPathMatcher
                                       (FileSystems/getDefault)
                                       (str "glob:" glob))
        ^PathMatcher exclude-matcher (when-let [excludes-glob (:excludes options)]
                                       (.getPathMatcher
                                         (FileSystems/getDefault)
                                         (str "glob:" excludes-glob)))]
    (remove-binary-files
      (->> starting-folder
           io/file
           file-seq
           (filter (fn [^File f] (.isFile f)))
           (filter (fn [^File f]
                     (if (or (.getParent glob-file) (re-find #"\*\*" glob))
                       (.matches grammar-matcher ^Path (.toPath ^File f))
                       (when (= starting-folder (str (.getParent (.toPath ^File f))))
                         (.matches grammar-matcher ^Path (.getFileName (.toPath ^File f)))))))
           (remove (fn [^File f]
                     (if exclude-matcher
                       (if (re-find #"\*\*" (:excludes options))
                         (.matches exclude-matcher ^Path (.toPath ^File f))
                         (when (= starting-folder (str (.getParent (.toPath ^File f))))
                           (.matches exclude-matcher ^Path (.getFileName (.toPath ^File f)))))
                       false)))
           (mapv #(.getPath ^File %)))
      options)))

(comment
  (lmgrep.fs/get-files "*.md" {})

  (lmgrep.fs/get-files "*.md" {:excludes "README.md"})
  (lmgrep.fs/get-files "**/*.md" {:excludes "README.md"})

  (lmgrep.fs/get-files "*.clj" {})
  (lmgrep.fs/get-files "./src/lmgrep/*.clj" {})
  (lmgrep.fs/get-files "**.clj" {})
  (lmgrep.fs/get-files "**.clj" {:excludes "**test*"})
  (lmgrep.fs/get-files "**/*.clj" {})
  (time (count (lmgrep.fs/get-files "**.*" {:skip-binary-files true})))
  (lmgrep.fs/get-files "/var/log/**.log" {}))
