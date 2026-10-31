package com.nadav.dlp.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

/** Checks boundary semantics and known token counts with reproducible, fixed-seed generated inputs. */
class KeywordMatcherTest {
    private final KeywordMatcher matcher = new KeywordMatcher();

    @Test
    void optionalKeywordDetailsAgreeWithTotalAndDoNotIncludeUnmatchedWords() {
        var result = matcher.inspect("Secret SECRET password secretary", List.of("secret", "password", "token"), true);
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.matchedKeywords()).containsExactly(
            new KeywordMatcher.KeywordCount("secret", 2), new KeywordMatcher.KeywordCount("password", 1));
        assertThat(matcher.inspect("secret", List.of("secret"), false).matchedKeywords()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"' secret ',1", "'secret.',1", "'secret,',1", "'secret/secret',2",
        "'secret@secret',2", "'secret😀secret',2", "'αsecret',0", "'secretβ',0",
        "'1secret',0", "'secret1',0", "'_secret',0", "'secret_',0",
        "'סודיsecret',0", "'secretסודי',0", "'secret-secret',2", "'SECRET',1",
        "'secret́',0", "'secrets',0", "'topsecret',0", "'secret;secret:secret',3"})
    void boundaryMatrix(String text, int expected) {
        assertThat(matcher.count(matcher.normalize(text), List.of("secret"))).isEqualTo(expected);
    }

    static Stream<Arguments> generatedTexts() {
        return IntStream.range(0, 30).mapToObj(seed -> {
            var random = new Random(seed);
            String[] tokens = {"secret", "PASSWORD", "SeCrEt", "secretary", "passwords", "_secret", "secret2", "ordinary"};
            String[] separators = {" ", "\n", "\t", "!", ",", "-", "😀"};
            var text = new StringBuilder();
            int expected = 0;
            for (int i = 0; i < 200; i++) {
                int choice = random.nextInt(tokens.length);
                if (choice < 3) expected++;
                text.append(tokens[choice]).append(separators[random.nextInt(separators.length)]);
            }
            return Arguments.of(seed, text.toString(), expected);
        });
    }

    @ParameterizedTest(name = "deterministic generated text seed {0}")
    @MethodSource("generatedTexts")
    void generatedTextsAgreeWithKnownTokenCounts(int seed, String text, int expected) {
        assertThat(matcher.count(text, List.of("secret", "password"))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"'secret SECRET Secret!',3", "'secretary topsecret secret2 _secret secret_',0",
        "'secret-secret (secret)',3", "'',0"})
    void countsWholeWordsIgnoringCase(String text, int expected) {
        assertThat(matcher.count(matcher.normalize(text), List.of("secret"))).isEqualTo(expected);
    }
    @Test
    void countsAllKeywordsAndRepeatedOccurrences() {
        assertThat(matcher.count("secret password password", List.of("secret", "password"))).isEqualTo(3);
    }
    @Test
    void treatsRegexMetacharactersLiterally() {
        assertThat(matcher.count("c++ c++! c+++", List.of("c++"))).isEqualTo(3);
        assertThat(matcher.count("a.b aXb", List.of("a.b"))).isEqualTo(1);
    }
    @Test
    void supportsHebrewAndUnicodeWordBoundaries() {
        assertThat(matcher.count("סודי סודי! הסודי סודיות", List.of("סודי"))).isEqualTo(2);
        assertThat(matcher.count("ésecret secreté secret", List.of("secret"))).isEqualTo(1);
    }
    @Test
    void normalizesCanonicallyEquivalentUnicode() {
        assertThat(matcher.count(matcher.normalize("cafe\u0301 CAFÉ"), List.of("café"))).isEqualTo(2);
    }
    @Test
    void countsPhrasesWithExactInternalWhitespace() {
        assertThat(matcher.count("top secret top  secret", List.of("top secret"))).isEqualTo(1);
    }
    @Test
    void distinctOverlappingKeywordsEachCount() {
        assertThat(matcher.count("top secret", List.of("secret", "top secret"))).isEqualTo(2);
    }
}
