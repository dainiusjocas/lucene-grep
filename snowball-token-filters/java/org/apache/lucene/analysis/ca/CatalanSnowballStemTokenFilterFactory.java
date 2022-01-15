package org.apache.lucene.analysis.ca;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.tartarus.snowball.ext.CatalanStemmer;

import java.util.Map;

public class CatalanSnowballStemTokenFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "catalanSnowballStem";

    /** Creates a new CatalanSnowballStemTokenFilterFactory */
    public CatalanSnowballStemTokenFilterFactory(Map<String,String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    /** Default ctor for compatibility with SPI */
    public CatalanSnowballStemTokenFilterFactory() {
        throw defaultCtorException();
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new SnowballFilter(input, new CatalanStemmer());
    }
}
