(ns lmgrep.predefined-analyzers
  (:require [lmgrep.features])
  (:import (org.apache.lucene.analysis.standard StandardAnalyzer)))

(defn stempel-analyzers []
  (when lmgrep.features/stempel?
    (require '[lmgrep.stempel])
    @(resolve 'lmgrep.stempel/analyzers)))

(defn bundled-analyzers []
  (when lmgrep.features/bundled?
    (require 'lmgrep.bundled)
    @(resolve 'lmgrep.bundled/analyzers)))

(def analyzers
  (merge
    (stempel-analyzers)
    (bundled-analyzers)
    {"standard" (StandardAnalyzer.)}))
