(ns lmgrep.stempel
  (:import (org.apache.lucene.analysis.pl PolishAnalyzer)))

(def analyzers
  {"polish" (PolishAnalyzer.)})
