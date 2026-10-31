package com.nadav.dlp.scan;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Counts literal whole-word occurrences without retaining request-specific state. */
@Component
public class KeywordMatcher {
    // A word consists of Unicode letters, marks, numbers and underscore.
    private static final String WORD = "[\\p{L}\\p{M}\\p{N}_]";

    /** Converts equivalent Unicode spellings to NFC before matching without changing word boundaries. */
    public String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /** Counts all occurrences of each configured literal, ignoring case and excluding embedded substrings. */
    public int count(String normalizedText, List<String> keywords) {
        return inspect(normalizedText, keywords, false).count();
    }

    public record KeywordCount(String keyword, int count) {}
    public record Inspection(int count, List<KeywordCount> matchedKeywords) {}

    /** Counts once; optional keyword totals explain results without returning submitted text. */
    public Inspection inspect(String normalizedText, List<String> keywords, boolean includeDetails) {
        int count = 0;
        var matches = new ArrayList<KeywordCount>();
        for (String keyword : keywords) {
            var pattern = Pattern.compile("(?<!" + WORD + ")" + Pattern.quote(keyword) + "(?!" + WORD + ")",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            var matcher = pattern.matcher(normalizedText);
            int keywordCount = 0;
            // Assumption: count occurrences, not distinct words. "secret secret" is two matches.
            while (matcher.find()) keywordCount++;
            count += keywordCount;
            if (includeDetails && keywordCount > 0) matches.add(new KeywordCount(keyword, keywordCount));
        }
        return new Inspection(count, List.copyOf(matches));
    }
}
