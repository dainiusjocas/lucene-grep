(ns lmgrep.fs
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.core.reducers :as r]
            [babashka.fs :as bfs])
  (:import (java.nio.file FileSystems PathMatcher Path Files LinkOption)
           (java.io File IOException)))

(def file-options (case (System/getProperty "os.name")
                    "Linux" "-ib"
                    "Mac OS X" "-I"
                    nil))

(defn binary-file? [^String file-path]
  (.contains ^String (:out (sh/sh "file" file-options file-path))
             "charset=binary"))

(defn filter-files [files]
  (r/filter (fn [^String file-path] (.isFile ^File (io/file file-path))) files))

(defn infer-root-folder
  "Take the lowest directory that does not contain wildcards.
  Peel back GLOB pattern up to the root directory"
  [^String glob]
  (loop [^File glob-file (io/file glob)]
    (let [^String pathname-parent (or (.getParent glob-file) ".")]
      (if-not (re-find #"\*\*?" pathname-parent)
        pathname-parent
        (recur (io/file pathname-parent))))))



; TODO: Support regex pattern
#_(defn get-files
    "Given GLOB string, infers what is the root dir and returns a list of paths
    to files whose name matches the GLOB.
    Options:
    - :excludes GLOB to excludes files
    - :skip-binary-files check if a file is a binary (only for linux and macos)"
    [^String glob options]
    (let [glob (str/replace glob #"^.\\" "")                  ; on Windows Terminal: .\README.md => README.md
          ^File glob-file (io/file glob)
          ^String root-folder (infer-root-folder glob)
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
        (->> root-folder
             io/file
             file-seq
             (r/filter (fn [^File f] (.isFile f)))
             (r/filter (fn [^File f]
                         (if (or (.getParent glob-file) (re-find #"\*\*" glob))
                           (.matches grammar-matcher ^Path (.toPath ^File f))
                           (when (= root-folder (str (.getParent (.toPath ^File f))))
                             (.matches grammar-matcher ^Path (.getFileName (.toPath ^File f)))))))
             (r/remove (fn [^File f]
                         (if exclude-matcher
                           (if (re-find #"\*\*" (:excludes options))
                             (.matches exclude-matcher ^Path (.toPath ^File f))
                             (when (= root-folder (str (.getParent (.toPath ^File f))))
                               (.matches exclude-matcher ^Path (.getFileName (.toPath ^File f)))))
                           false)))
             (r/map (fn [^File f] (.getPath f)))
             (r/remove binary-file-pred-fn)
             (r/foldcat)))))

(def windows?
  (-> (System/getProperty "os.name")
      (str/lower-case)
      (str/includes? "win")))

(defn fs-glob
  "Given a file and glob pattern, returns matches as vector of
  files. Patterns containing ** or / will cause a recursive walk over
  path. Glob interpretation is done using the rules described in
  https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String).

  Options:

  - :hidden: match hidden files. Note: on Windows files starting with
  a dot are not hidden, unless their hidden attribute is set.
  - :follow-links: follow symlinks.
  - :handle-error: just skip the problematic (e.g. no permission) files.
  - :only-files: remove directories from the result"
  ([root pattern] (fs-glob root pattern nil))
  ([root pattern {:keys [hidden follow-links max-depth
                         handle-error only-files skip-binary-files
                         excludes]}]
   (let [base-path (-> root bfs/absolutize bfs/normalize)
         base-path (if windows?
                     (str/replace base-path bfs/file-separator (str "\\" bfs/file-separator))
                     base-path)
         skip-hidden? (not hidden)
         results (atom (transient []))
         past-root? (volatile! nil)
         [base-path pattern recursive]
         (let [recursive (or (str/includes? pattern "**")
                             (str/includes? pattern bfs/file-separator))
               pattern (str base-path
                            ;; we need to escape the file separator on Windows
                            (when windows? "\\")
                            bfs/file-separator
                            pattern)]
           [base-path pattern recursive])
         matcher (.getPathMatcher
                   (FileSystems/getDefault)
                   (str "glob:" pattern))
         excludes-matcher (when excludes
                            (.getPathMatcher
                              (FileSystems/getDefault)
                              (str "glob:" (str base-path
                                                ;; we need to escape the file separator on Windows
                                                (when windows? "\\")
                                                bfs/file-separator
                                                excludes))))
         match (fn [^Path path]
                 (if (and (.matches matcher path)
                          (not (and excludes-matcher (.matches excludes-matcher path))))
                   (when
                     (or (Files/isRegularFile path (make-array LinkOption 0))
                         (and (Files/isDirectory path (make-array LinkOption 0))
                              (false? only-files)))
                     (when-not (and skip-binary-files (binary-file? (str path)))
                       (swap! results conj! path)))
                   nil))]
     (bfs/walk-file-tree
       base-path
       {:max-depth         max-depth
        :follow-links      follow-links
        :pre-visit-dir     (fn [dir _attrs]
                             (if (and @past-root?
                                      (or (not recursive)
                                          (and skip-hidden?
                                               (bfs/hidden? dir))))
                               :skip-subtree
                               (do
                                 (if @past-root? (match dir)
                                                 (vreset! past-root? true))
                                 :continue)))
        :visit-file        (fn [path _attrs]
                             (when-not (and skip-hidden?
                                            (bfs/hidden? path))
                               (match path))
                             :continue)
        :visit-file-failed (if handle-error
                             (fn [path ^IOException exception]
                               (when (System/getenv "DEBUG_MODE")
                                 (.println System/err (format "Visiting %s failed: %s" path (type exception))))
                               :skip-subtree)
                             nil)})
     (let [results (persistent! @results)
           absolute-cwd (bfs/absolutize "")]
       (if (bfs/relative? root)
         (mapv #(bfs/relativize absolute-cwd %)
               results)
         results)))))

(defn get-files [^String glob options]
  (let [^String root-folder (infer-root-folder glob)
        glob-pattern (if (= "." root-folder)
                       glob
                       (str/replace glob (re-pattern (format "%s/?" root-folder)) ""))]
    (mapv str (fs-glob root-folder
                       glob-pattern
                       (merge {:hidden            true
                               :follow-links      false
                               :max-depth         Integer/MAX_VALUE
                               :handle-error      true
                               :only-files        true
                               :skip-binary-files false}
                              options)))))

(comment
  (lmgrep.fs/get-files "*.md" {})
  (lmgrep.fs/files-get "*.md" {})
  (lmgrep.fs/files-get "**.md" {})

  (time (lmgrep.fs/get-files "/home/dj/vinted/**/.env" {}))
  (time (lmgrep.fs/files-get "/home/dj/vinted/**/.env" {}))
  (time (lmgrep.fs/files-get "/home/dj/**/.env" {}))

  (lmgrep.fs/files-get "/home/dj/.zshrc" {})
  (lmgrep.fs/files-get "**/lucene" {})
  (lmgrep.fs/get-files "**/lucene" {})

  (lmgrep.fs/get-files "docs/**.png" {:skip-binary-files true})
  (lmgrep.fs/files-get "docs/**.png" {:skip-binary-files true})
  (lmgrep.fs/files-get "docs/**.png" {:skip-binary-files false})




  (lmgrep.fs/get-files "*.md" {:excludes "README.md"})
  (lmgrep.fs/files-get "*.md" {:excludes "README.md"})
  (lmgrep.fs/files-get "*.md" {})
  (lmgrep.fs/get-files "**/*.md" {:excludes "README.md"})
  (lmgrep.fs/files-get "**/*.md" {:excludes "README.md"})
  (lmgrep.fs/files-get "**/*.md" {:excludes "**/README.md"})
  (lmgrep.fs/files-get "**.md" {})

  (lmgrep.fs/get-files "*.clj" {})
  (lmgrep.fs/get-files "./src/lmgrep/*.clj" {})
  (lmgrep.fs/get-files "**.clj" {})
  (lmgrep.fs/get-files "**.clj" {:excludes "**test*"})
  (lmgrep.fs/get-files "**/*.clj" {})
  (time (count (lmgrep.fs/get-files "**.*" {:skip-binary-files true})))
  (lmgrep.fs/get-files "/var/log/**.log" {}))
