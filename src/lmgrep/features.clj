(ns lmgrep.features)

;; excluded by default
(def raudikko? (= "true" (System/getenv "LMGREP_FEATURE_RAUDIKKO")))
