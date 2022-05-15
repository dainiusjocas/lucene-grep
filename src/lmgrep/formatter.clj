(ns lmgrep.formatter
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [lmgrep.ansi-escapes :as ansi]))

; highlights should be reconsiliated:
; - back-to-back highlights should be joined into one
(defn cutout-highlight [^String input-text highlights options]
  (let [highlight-fn (if (and (string? (:pre-tags options)) (string? (:post-tags options)))
                       #(str (:pre-tags options) % (:post-tags options))
                       ansi/red-text)]
    (->> highlights
         (mapv (fn [highlight]
                 (let [prefix (subs input-text
                                    (max 0 (- (:begin-offset highlight) 20))
                                    (:begin-offset highlight))
                       suffix (subs input-text
                                    (:end-offset highlight)
                                    (min (+ (:end-offset highlight) 20) (.length input-text)))
                       matched-text (highlight-fn
                                      (subs input-text
                                            (:begin-offset highlight)
                                            (:end-offset highlight)))
                       ; Matching over the \newline is now possible
                       resp (str/replace (str prefix matched-text suffix) "\n" " ")]
                   resp)))
         (str/join "\n"))))

(defn highlight-line [line-str highlights options]
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
                            (:end-offset ann)))))))))

(defn highlight-text
  "TODO: overlapping phrase highlights are combined under one color, maybe solve it?"
  [line-str highlights options]
  (if (:with-score options)
    line-str
    (when (seq highlights)
      (if (:split options)
        (highlight-line line-str highlights options)
        (cutout-highlight line-str highlights options)))))

(defn file-string [file line-number options]
  (if (and (:hyperlink options) file)
    (ansi/link file (str (.toURI (io/file file)) "#" line-number))
    file))

(defn string-output [highlights details options]
  (if-let [template (:template options)]
    (if (str/blank? template)
      ""
      (-> template
          (str/replace "{{file}}" (or (file-string (:file details) (:line-number details) options) ""))
          (str/replace "{{line-number}}" (str (:line-number details)))
          (str/replace "{{highlighted-line}}" (highlight-text (:line details) highlights options))
          (str/replace "{{line}}" (:line details))
          (str/replace "{{score}}" (str (:score details)))))
    (if (:score details)
      (format "%s:%s:%s:%s"
              (ansi/purple-text (or (file-string (:file details) (:line-number details) options) "*STDIN*"))
              (ansi/green-text (:line-number details))
              (ansi/purple-text (:score details))
              (highlight-text (:line details) highlights options))
      (format "%s:%s:%s"
              (ansi/purple-text (or (file-string (:file details) (:line-number details) options) "*STDIN*"))
              (ansi/green-text (:line-number details))
              (highlight-text (:line details) highlights options)))))
