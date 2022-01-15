package org.apache.lucene.analysis.ga;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.tartarus.snowball.ext.IrishStemmer;

import java.util.Map;

public class IrishSnowballStemTokenFilterFactory extends TokenFilterFactory {

    /** SPI name */
    public static final String NAME = "irishSnowballStem";

    /** Creates a new IrishSnowballStemTokenFilterFactory */
    public IrishSnowballStemTokenFilterFactory(Map<String,String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    public IrishSnowballStemTokenFilterFactory() {
        throw defaultCtorException();
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new SnowballFilter(input, new IrishStemmer());
    }
}
