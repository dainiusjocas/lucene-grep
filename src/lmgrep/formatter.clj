(ns lmgrep.formatter
  (:require [lmgrep.ansi-escapes :as ansi]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn highlight-line
  "TODO: overlapping phrase highlights are combined under one color, maybe solve it?"
  [line-str highlights options]
  (if (:with-score options)
    line-str
    (when (seq highlights)
      (let [highlights (sort-by :begin-offset highlights)
            highlight-fn (if (and (string? (:pre-tags options)) (string? (:post-tags options)))
                           #(str (:pre-tags options) % (:post-tags options))
                           ansi/red-text)]
        (loop [[[ann next-ann] & ann-pairs] (partition 2 1 nil highlights)
               acc ""
               last-position 0]
          (let [prefix (subs line-str last-position (max last-position (:begin-offset ann)))
                highlight (let [text-to-highlight (subs line-str (:begin-offset ann) (:end-offset ann))]
                            (if (< (:begin-offset ann) last-position)
                              ; adjusting highlight text for overlap
                              (highlight-fn (subs text-to-highlight (min (.length text-to-highlight)
                                                                         (- last-position (:begin-offset ann)))))
                              (highlight-fn text-to-highlight)))
                suffix (if (nil? next-ann)
                         (subs line-str (:end-offset ann))
                         (subs line-str (:end-offset ann) (max (:begin-offset next-ann)
                                                               (:end-offset ann))))]
            (if (nil? next-ann)
              (str acc prefix highlight suffix)
              (recur ann-pairs
                     (str acc prefix highlight suffix)
                     (long (max (:begin-offset next-ann)
                                (:end-offset ann)))))))))))

(defn file-string [file options]
  (if (:hyperlink options)
    (ansi/link file (str (.toURI (io/file file))))
    file))

(defn string-output [highlights {:keys [file line-number line score]} options]
  (if-let [template (:template options)]
    (if (str/blank? template)
      ""
      (-> template
          (str/replace "{{file}}" (or (file-string file options) ""))
          (str/replace "{{line-number}}" (str line-number))
          (str/replace "{{highlighted-line}}" (highlight-line line highlights options))
          (str/replace "{{line}}" line)
          (str/replace "{{score}}" (str score))))
    (if score
      (format "%s:%s:%s:%s"
              (ansi/purple-text (or (file-string file options) "*STDIN*"))
              (ansi/green-text line-number)
              (ansi/purple-text score)
              (highlight-line line highlights options))
      (format "%s:%s:%s"
              (ansi/purple-text (or (file-string file options) "*STDIN*"))
              (ansi/green-text line-number)
              (highlight-line line highlights options)))))
