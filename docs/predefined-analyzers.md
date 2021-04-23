# Predefined Analyzers

Provide the predefined analyzer with `--analysis` flag, e.g:
```shell
echo "dogs and cats" | \
  ./lmgrep \
  --only-analyze \
  --analysis='{"analyzer": {"name": "English"}}'
```

NOTE: predefined analyzers doesn't support additional parameters. 
In the future this will [change](https://github.com/dainiusjocas/lucene-grep/issues/82).

NOTE: name is case-insensitive.

There are 45 analyzers available:

|             Name |                                                    Class |
|------------------:|:----------------------------------------------------------|
|           catalan |             org.apache.lucene.analysis.ca.CatalanAnalyzer |
|              stop |              org.apache.lucene.analysis.core.StopAnalyzer |
|           persian |             org.apache.lucene.analysis.fa.PersianAnalyzer |
|        lithuanian |          org.apache.lucene.analysis.lt.LithuanianAnalyzer |
|           finnish |             org.apache.lucene.analysis.fi.FinnishAnalyzer |
|          standard |      org.apache.lucene.analysis.standard.StandardAnalyzer |
|           italian |             org.apache.lucene.analysis.it.ItalianAnalyzer |
|         hungarian |           org.apache.lucene.analysis.hu.HungarianAnalyzer |
|          estonian |            org.apache.lucene.analysis.et.EstonianAnalyzer |
|        indonesian |          org.apache.lucene.analysis.id.IndonesianAnalyzer |
|        portuguese |          org.apache.lucene.analysis.pt.PortugueseAnalyzer |
|           spanish |             org.apache.lucene.analysis.es.SpanishAnalyzer |
|               cjk |                org.apache.lucene.analysis.cjk.CJKAnalyzer |
|             irish |               org.apache.lucene.analysis.ga.IrishAnalyzer |
|           classic |       org.apache.lucene.analysis.standard.ClassicAnalyzer |
| unicodewhitespace | org.apache.lucene.analysis.core.UnicodeWhitespaceAnalyzer |
|           latvian |             org.apache.lucene.analysis.lv.LatvianAnalyzer |
|          armenian |            org.apache.lucene.analysis.hy.ArmenianAnalyzer |
|          galician |            org.apache.lucene.analysis.gl.GalicianAnalyzer |
|              thai |                org.apache.lucene.analysis.th.ThaiAnalyzer |
|            danish |              org.apache.lucene.analysis.da.DanishAnalyzer |
|           keyword |           org.apache.lucene.analysis.core.KeywordAnalyzer |
|            polish |              org.apache.lucene.analysis.pl.PolishAnalyzer |
|          romanian |            org.apache.lucene.analysis.ro.RomanianAnalyzer |
|           turkish |             org.apache.lucene.analysis.tr.TurkishAnalyzer |
|         norwegian |           org.apache.lucene.analysis.no.NorwegianAnalyzer |
|         bulgarian |           org.apache.lucene.analysis.bg.BulgarianAnalyzer |
|            german |              org.apache.lucene.analysis.de.GermanAnalyzer |
|      collationkey |          org.apache.lucene.collation.CollationKeyAnalyzer |
|           russian |             org.apache.lucene.analysis.ru.RussianAnalyzer |
|             dutch |               org.apache.lucene.analysis.nl.DutchAnalyzer |
|            basque |              org.apache.lucene.analysis.eu.BasqueAnalyzer |
|         brazilian |           org.apache.lucene.analysis.br.BrazilianAnalyzer |
|     uax29urlemail | org.apache.lucene.analysis.standard.UAX29URLEmailAnalyzer |
|           swedish |             org.apache.lucene.analysis.sv.SwedishAnalyzer |
|        whitespace |        org.apache.lucene.analysis.core.WhitespaceAnalyzer |
|            simple |            org.apache.lucene.analysis.core.SimpleAnalyzer |
|           english |             org.apache.lucene.analysis.en.EnglishAnalyzer |
|            french |              org.apache.lucene.analysis.fr.FrenchAnalyzer |
|             czech |               org.apache.lucene.analysis.cz.CzechAnalyzer |
|             greek |               org.apache.lucene.analysis.el.GreekAnalyzer |
|           bengali |             org.apache.lucene.analysis.bn.BengaliAnalyzer |
|             hindi |               org.apache.lucene.analysis.hi.HindiAnalyzer |
|            arabic |              org.apache.lucene.analysis.ar.ArabicAnalyzer |
|            sorani |             org.apache.lucene.analysis.ckb.SoraniAnalyzer |

To learn more about each analyzer Google for `Lucene <Analyzer Class>`.
