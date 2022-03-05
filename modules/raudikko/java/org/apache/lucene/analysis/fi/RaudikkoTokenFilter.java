package org.apache.lucene.analysis.fi;

import fi.evident.raudikko.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public final class RaudikkoTokenFilter extends TokenFilter {

    private State current;
    private final Analyzer raudikkoAnalyzer;
    private final boolean analyzeAll;
    private final int minimumWordSize;
    private final int maximumWordSize;

    private final CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);

    private final Deque<String> alternatives = new ArrayDeque<>();

    private static final Pattern VALID_WORD_PATTERN = Pattern.compile("[a-zA-ZåäöÅÄÖ-]+");

    public RaudikkoTokenFilter(TokenStream input, Analyzer analyzer, boolean analyzeAll, int minimumWordSize, int maximumWordSize) {
        super(input);
        raudikkoAnalyzer = analyzer;
        this.analyzeAll = analyzeAll;
        this.minimumWordSize = minimumWordSize;
        this.maximumWordSize = maximumWordSize;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!alternatives.isEmpty()) {
            outputAlternative(alternatives.removeFirst());
            return true;
        }

        if (input.incrementToken()) {
            analyzeToken();
            return true;
        }

        return false;
    }

    private void analyzeToken() {
        if (!isCandidateForAnalysis(charTermAttribute))
            return;

        List<String> baseForms = analyze(charTermAttribute);
        if (baseForms.isEmpty())
            return;

        charTermAttribute.setEmpty().append(baseForms.get(0));

        if (this.analyzeAll && baseForms.size() > 1) {
            current = captureState();

            alternatives.addAll(baseForms.subList(1, baseForms.size()));
        }
    }

    private List<String> analyze(CharSequence wordSeq) {
        String word = wordSeq.toString();
        return analyzeUncached(word);
    }

    private List<String> analyzeUncached(String word) {
        List<String> results = raudikkoAnalyzer.baseForms(word);

        switch (results.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(results.get(0));
            default:
                return new ArrayList<>(results);
        }
    }

    private void outputAlternative(String token) {
        restoreState(current);

        positionIncrementAttribute.setPositionIncrement(0);
        charTermAttribute.setEmpty().append(token);
    }

    private boolean isCandidateForAnalysis(CharSequence word) {
        return word.length() >= this.minimumWordSize && word.length() <= this.maximumWordSize && VALID_WORD_PATTERN.matcher(word).matches();
    }
}
