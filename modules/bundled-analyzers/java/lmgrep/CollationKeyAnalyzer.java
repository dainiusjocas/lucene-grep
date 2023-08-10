package lmgrep;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.collation.CollationAttributeFactory;
import org.apache.lucene.util.AttributeFactory;

import java.text.Collator;

//Just a dummy clone of the original CollationKeyAnalyzer with a zero argument constructor
// to support SPI
public class CollationKeyAnalyzer extends Analyzer {
    private final CollationAttributeFactory factory;

    public CollationKeyAnalyzer() {
        Collator collator = Collator.getInstance();
        this.factory = new CollationAttributeFactory(collator);
    }

    @Override
    protected AttributeFactory attributeFactory(String fieldName) {
        return factory;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        KeywordTokenizer tokenizer =
                new KeywordTokenizer(factory, KeywordTokenizer.DEFAULT_BUFFER_SIZE);
        return new TokenStreamComponents(tokenizer, tokenizer);
    }

}
