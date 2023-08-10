package lmgrep;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LetterTokenizer;

import java.util.ArrayList;

// Just a dummy clone to prevent breaking changes and to support SPI.
public class StopAnalyzer extends Analyzer {

    private final CharArraySet stopwords;
    public StopAnalyzer() {
        this.stopwords = new CharArraySet(new ArrayList<>(), true);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new LetterTokenizer();
        return new TokenStreamComponents(
                source, new StopFilter(new LowerCaseFilter(source), stopwords));
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }
}
