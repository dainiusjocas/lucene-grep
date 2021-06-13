(ns lmgrep.fs
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.core.reducers :as r])
  (:import (java.nio.file FileSystems PathMatcher Path)
           (java.io File)))

(def file-options (case (System/getProperty "os.name")
                    "Linux" "-ib"
                    "Mac OS X" "-I"
                    nil))

(defn binary-file? [^String file-path]
  (.contains ^String (:out (sh/sh "file" file-options file-path))
             "charset=binary"))

(defn filter-files [files]
  (r/filter (fn [^String file-path] (.isFile ^File (io/file file-path))) files))

; TODO: Support regex pattern
(defn get-files [^String glob options]
  (let [glob (str/replace glob #"^.\\" "")
        glob-file (io/file glob)
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
                                         (str "glob:" excludes-glob)))
        binary-file-pred-fn (fn [^String file-path]
                              (and (get options :skip-binary-files)
                                   file-options
                                   (binary-file? file-path)))]
    (if (.isFile glob-file)
      [(.getPath glob-file)]
      (->> starting-folder
           io/file
           file-seq
           (r/filter (fn [^File f] (.isFile f)))
           (r/filter (fn [^File f]
                       (if (or (.getParent glob-file) (re-find #"\*\*" glob))
                         (.matches grammar-matcher ^Path (.toPath ^File f))
                         (when (= starting-folder (str (.getParent (.toPath ^File f))))
                           (.matches grammar-matcher ^Path (.getFileName (.toPath ^File f)))))))
           (r/remove (fn [^File f]
                       (if exclude-matcher
                         (if (re-find #"\*\*" (:excludes options))
                           (.matches exclude-matcher ^Path (.toPath ^File f))
                           (when (= starting-folder (str (.getParent (.toPath ^File f))))
                             (.matches exclude-matcher ^Path (.getFileName (.toPath ^File f)))))
                         false)))
           (r/map (fn [^File f] (.getPath f)))
           (r/remove binary-file-pred-fn)
           (r/foldcat)))))

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
