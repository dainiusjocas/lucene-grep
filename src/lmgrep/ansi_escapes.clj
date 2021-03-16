(ns lmgrep.ansi-escapes)

(defn red-text [text]
  (str \ "[1;31m" text \ "[0m"))

(defn purple-text [text]
  (str \ "[0;35m" text \ "[0m"))

(def SEP ";")
(def BEL "\u0007")
(def OSC "\u001B]")

(defn link [text url]
  (str OSC "8" SEP SEP url BEL text OSC "8" SEP SEP BEL))

(defn green-text [text]
  (str \ "[0;32m" text \ "[0m"))
