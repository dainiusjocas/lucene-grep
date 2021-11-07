(ns lmgrep.lucene.predefined-analyzers
  (:import (java.util Locale ArrayList)
           (java.text Collator)
           (org.apache.lucene.analysis.ar ArabicAnalyzer)
           (org.apache.lucene.analysis.bg BulgarianAnalyzer)
           (org.apache.lucene.analysis.bn BengaliAnalyzer)
           (org.apache.lucene.analysis.br BrazilianAnalyzer)
           (org.apache.lucene.analysis.ca CatalanAnalyzer)
           (org.apache.lucene.analysis.cjk CJKAnalyzer)
           (org.apache.lucene.analysis.ckb SoraniAnalyzer)
           (org.apache.lucene.analysis CharArraySet)
           (org.apache.lucene.analysis.core StopAnalyzer UnicodeWhitespaceAnalyzer WhitespaceAnalyzer KeywordAnalyzer SimpleAnalyzer)
           (org.apache.lucene.analysis.cz CzechAnalyzer)
           (org.apache.lucene.analysis.da DanishAnalyzer)
           (org.apache.lucene.analysis.nl DutchAnalyzer)
           (org.apache.lucene.analysis.de GermanAnalyzer)
           (org.apache.lucene.analysis.el GreekAnalyzer)
           (org.apache.lucene.analysis.es SpanishAnalyzer)
           (org.apache.lucene.analysis.et EstonianAnalyzer)
           (org.apache.lucene.analysis.eu BasqueAnalyzer)
           (org.apache.lucene.analysis.fa PersianAnalyzer)
           (org.apache.lucene.analysis.fi FinnishAnalyzer)
           (org.apache.lucene.analysis.fr FrenchAnalyzer)
           (org.apache.lucene.analysis.ga IrishAnalyzer)
           (org.apache.lucene.analysis.gl GalicianAnalyzer)
           (org.apache.lucene.analysis.hi HindiAnalyzer)
           (org.apache.lucene.analysis.hu HungarianAnalyzer)
           (org.apache.lucene.analysis.hy ArmenianAnalyzer)
           (org.apache.lucene.analysis.id IndonesianAnalyzer)
           (org.apache.lucene.analysis.it ItalianAnalyzer)
           (org.apache.lucene.analysis.lt LithuanianAnalyzer)
           (org.apache.lucene.analysis.lv LatvianAnalyzer)
           (org.apache.lucene.analysis.no NorwegianAnalyzer)
           (org.apache.lucene.analysis.pt PortugueseAnalyzer)
           (org.apache.lucene.analysis.ro RomanianAnalyzer)
           (org.apache.lucene.analysis.ru RussianAnalyzer)
           (org.apache.lucene.analysis.standard ClassicAnalyzer UAX29URLEmailAnalyzer StandardAnalyzer)
           (org.apache.lucene.analysis.sv SwedishAnalyzer)
           (org.apache.lucene.analysis.th ThaiAnalyzer)
           (org.apache.lucene.analysis.tr TurkishAnalyzer)
           (org.apache.lucene.analysis.en EnglishAnalyzer)
           (org.apache.lucene.analysis.pl PolishAnalyzer)
           (org.apache.lucene.collation CollationKeyAnalyzer)))

(def analyzers
  {"ArabicAnalyzer"            (ArabicAnalyzer.)
   "BulgarianAnalyzer"         (BulgarianAnalyzer.)
   "BengaliAnalyzer"           (BengaliAnalyzer.)
   "BrazilianAnalyzer"         (BrazilianAnalyzer.)
   "CatalanAnalyzer"           (CatalanAnalyzer.)
   "CJKAnalyzer"               (CJKAnalyzer.)
   "SoraniAnalyzer"            (SoraniAnalyzer.)
   "StopAnalyzer"              (StopAnalyzer. (CharArraySet. (ArrayList.) true))
   "CzechAnalyzer"             (CzechAnalyzer.)
   "DanishAnalyzer"            (DanishAnalyzer.)
   "DutchAnalyzer"             (DutchAnalyzer.)
   "GermanAnalyzer"            (GermanAnalyzer.)
   "GreekAnalyzer"             (GreekAnalyzer.)
   "SpanishAnalyzer"           (SpanishAnalyzer.)
   "EstonianAnalyzer"          (EstonianAnalyzer.)
   "BasqueAnalyzer"            (BasqueAnalyzer.)
   "PersianAnalyzer"           (PersianAnalyzer.)
   "FinnishAnalyzer"           (FinnishAnalyzer.)
   "FrenchAnalyzer"            (FrenchAnalyzer.)
   "IrishAnalyzer"             (IrishAnalyzer.)
   "GalicianAnalyzer"          (GalicianAnalyzer.)
   "HindiAnalyzer"             (HindiAnalyzer.)
   "HungarianAnalyzer"         (HungarianAnalyzer.)
   "ArmenianAnalyzer"          (ArmenianAnalyzer.)
   "IndonesianAnalyzer"        (IndonesianAnalyzer.)
   "ItalianAnalyzer"           (ItalianAnalyzer.)
   "LithuanianAnalyzer"        (LithuanianAnalyzer.)
   "LatvianAnalyzer"           (LatvianAnalyzer.)
   "NorwegianAnalyzer"         (NorwegianAnalyzer.)
   "PortugueseAnalyzer"        (PortugueseAnalyzer.)
   "RomanianAnalyzer"          (RomanianAnalyzer.)
   "RussianAnalyzer"           (RussianAnalyzer.)
   "ClassicAnalyzer"           (ClassicAnalyzer.)
   "UAX29URLEmailAnalyzer"     (UAX29URLEmailAnalyzer.)
   "SwedishAnalyzer"           (SwedishAnalyzer.)
   "ThaiAnalyzer"              (ThaiAnalyzer.)
   "TurkishAnalyzer"           (TurkishAnalyzer.)
   "EnglishAnalyzer"           (EnglishAnalyzer.)
   ;; add 13 MB to the binary
   "PolishAnalyzer"            (PolishAnalyzer.)
   "StandardAnalyzer"          (StandardAnalyzer.)
   "UnicodeWhitespaceAnalyzer" (UnicodeWhitespaceAnalyzer.)
   "WhitespaceAnalyzer"        (WhitespaceAnalyzer.)
   "KeywordAnalyzer"           (KeywordAnalyzer.)
   "SimpleAnalyzer"            (SimpleAnalyzer.)
   "CollationKeyAnalyzer"      (CollationKeyAnalyzer. (Collator/getInstance Locale/ENGLISH))})
