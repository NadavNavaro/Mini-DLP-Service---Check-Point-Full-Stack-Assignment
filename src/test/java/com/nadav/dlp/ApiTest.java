package com.nadav.dlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the public HTTP contract on H2; PostgreSQL reuses these tests unchanged via inheritance. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static final String[] DEMO_KEYWORDS = {"password", "secret", "confidential", "token",
        "credential", "apikey", "privatekey", "classified"};

    @Test
    void unicodeCaseMatchingPreservesKeywordsWithoutExpandingTheirLength() throws Exception {
        String keyword = "İ".repeat(100);
        var rule = type(5, keyword, "σ");
        assertThat(rule.at("/content/0").asText()).isEqualTo(keyword);
        var result = scan(dataset(rule.get("id").asText()), keyword + " " + "i".repeat(100) + " Σ σ ς");
        assertThat(result.at("/detected_objects/0/match_count").asInt()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[\"σ\",\"ς\"]", "[\"I\",\"ı\"]"})
    void unicodeCaseEquivalentKeywordsCannotInflateCounts(String words) throws Exception {
        String body = "{\"name\":\"Unicode\",\"description\":\"\",\"type\":\"keywords\",\"threshold\":1,\"content\":" + words + "}";
        mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    private JsonNode detailedScan(String id, String text, Object includeDetails) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("text", text);
        body.put("data_set_id", id);
        body.put("include_details", includeDetails);
        var response = mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn();
        return json.readTree(response.getResponse().getContentAsString());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"'Secret secret password secretary',3,true",
        "'secret password',2,false", "'secretary passwords',0,false", "'',0,false"})
    void optionalDetailsExplainExactCountsAndThresholds(String text, int count, boolean matched) throws Exception {
        String dt = type(3, "secret", "password").get("id").asText();
        String ds = dataset(dt);
        var result = detailedScan(ds, text, true);
        assertThat(result.get("status").asText()).isEqualTo(matched ? "match" : "not matched");
        assertThat(result.at("/inspection_details/data_set_id").asText()).isEqualTo(ds);
        var detail = result.at("/inspection_details/rules/0");
        assertThat(detail.get("id").asText()).isEqualTo(dt);
        assertThat(detail.get("threshold").asInt()).isEqualTo(3);
        assertThat(detail.get("match_count").asInt()).isEqualTo(count);
        assertThat(detail.get("matched").asBoolean()).isEqualTo(matched);
        assertThat(detail.get("reason").asText()).contains("count " + count)
            .contains(matched ? "at least threshold 3" : "below threshold 3");
        int sum = 0;
        for (var word : detail.get("matched_keywords")) {
            assertThat(word.get("keyword").asText()).isIn("secret", "password");
            assertThat(word.get("match_count").asInt()).isPositive();
            sum += word.get("match_count").asInt();
        }
        assertThat(sum).isEqualTo(count);
        if (count == 3) {
            assertThat(detail.at("/matched_keywords/0/keyword").asText()).isEqualTo("secret");
            assertThat(detail.at("/matched_keywords/0/match_count").asInt()).isEqualTo(2);
        }
        assertThat(result.has("text")).isFalse();
        assertThat(result.at("/inspection_details").has("text")).isFalse();
    }

    @Test
    void detailsIncludeFailingRulesWithoutCombiningTheirThresholds() throws Exception {
        String first = type(3, "secret").get("id").asText();
        String second = type(1, "token").get("id").asText();
        var result = detailedScan(dataset(first, second), "secret secret token", true);
        assertThat(result.get("detected_objects").size()).isEqualTo(1);
        assertThat(result.at("/detected_objects/0/id").asText()).isEqualTo(second);
        assertThat(result.at("/inspection_details/rules").size()).isEqualTo(2);
        for (var detail : result.at("/inspection_details/rules")) {
            assertThat(detail.get("matched").asBoolean()).isEqualTo(detail.get("id").asText().equals(second));
        }
        assertThat(result.at("/inspection_details/reason").asText()).contains("OR logic");
    }

    @Test
    void emptyPolicyExplanationContainsNoInventedRule() throws Exception {
        var result = detailedScan(dataset(), "secret", true);
        assertThat(result.get("status").asText()).isEqualTo("not matched");
        assertThat(result.at("/inspection_details/rules").size()).isZero();
        assertThat(result.at("/inspection_details/reason").asText()).contains("contains no Data Types");
    }

    @Test
    void detailsAreOmittedByDefaultAndWhenDisabledOrNull() throws Exception {
        String ds = dataset(type(1, "secret").get("id").asText());
        assertThat(scan(ds, "secret").has("inspection_details")).isFalse();
        assertThat(detailedScan(ds, "secret", false).has("inspection_details")).isFalse();
        assertThat(detailedScan(ds, "secret", null).has("inspection_details")).isFalse();
        assertThat(detailedScan(ds, "ordinary", false).toString()).isEqualTo("{\"status\":\"not matched\"}");
    }

    @Test
    void nonBooleanDetailsFlagIsRejected() throws Exception {
        mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("text", "secret", "data_set_id", UUID.randomUUID(),
                "include_details", "true")))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "'ordinary public information',0,3", "'Secret',1,1", "'password SECRET',2,3",
        "'secret secret password',3,3", "'SECRET Secret secret SECRET',4,3",
        "'PASSWORD secret Confidential TOKEN credential APIKEY PrivateKey CLASSIFIED',8,8",
        "'password secret confidential token credential apikey privatekey classified secret password',10,8",
        "'passwords secretary confidentiality tokens credentials apikeys privatekeys unclassified',0,1",
        "'password1 _secret confidential_ tokenized credential2 apikey123 privatekeyX classified9',0,1",
        "'password! (secret), confidential; token: credential/apikey privatekey-classified',8,8",
        "'secret secret',2,2", "'secret secret',2,3"
    })
    void demoKeywordsCountWholeWordOccurrencesAgainstDifferentThresholds(String text, int count, int threshold)
            throws Exception {
        String dt = type(threshold, DEMO_KEYWORDS).get("id").asText();
        var result = scan(dataset(dt), text);
        assertThat(result.get("status").asText()).isEqualTo(count >= threshold ? "match" : "not matched");
        if (count >= threshold) {
            assertThat(result.get("detected_objects").size()).isEqualTo(1);
            assertThat(result.at("/detected_objects/0/match_count").asInt()).isEqualTo(count);
        } else {
            assertThat(result.has("detected_objects")).isFalse();
        }
    }

    static Stream<String> invalidRuleBodies() {
        var valid = "{\"name\":\"Rule\",\"description\":\"Demo\",\"type\":\"keywords\",\"content\":[\"secret\"],\"threshold\":1}";
        return Stream.of(
            valid.replace("\"Rule\"", "null"),
            valid.replace("\"Rule\"", "\"\""),
            valid.replace("\"Rule\"", "\"" + "a".repeat(121) + "\""),
            valid.replace("\"Demo\"", "null"),
            valid.replace("\"Demo\"", "\"" + "a".repeat(1001) + "\""),
            valid.replace("\"keywords\"", "null"),
            valid.replace("\"keywords\"", "\"regex\""),
            valid.replace("[\"secret\"]", "null"),
            valid.replace("[\"secret\"]", "[]"),
            valid.replace("[\"secret\"]", "[null]"),
            valid.replace("[\"secret\"]", "[\"\"]"),
            valid.replace("[\"secret\"]", "[\"   \"]"),
            valid.replace("[\"secret\"]", "[\"secret\",\"SECRET\"]"),
            valid.replace("[\"secret\"]", "[\"" + "a".repeat(101) + "\"]"),
            valid.replace("[\"secret\"]", "[\"café\",\"cafe\u0301\"]"),
            valid.replace(":1}", ":null}"),
            valid.replace(":1}", ":0}"),
            valid.replace(":1}", ":-1}"),
            valid.replace(":1}", ":1.5}"),
            valid.replace(":1}", ":\"1\"}"),
            valid.replace(":1}", ":true}"),
            valid.replace(":1}", ":2147483648}"),
            valid.replace("\"name\":", "\"unexpected\":"));
    }

    @ParameterizedTest(name = "invalid rule payload {index}")
    @MethodSource("invalidRuleBodies")
    void invalidRulePayloadsHaveConsistentErrors(String body) throws Exception {
        mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value("/api/datatypes"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/datatypes?page=-1", "/api/datatypes?size=0", "/api/datatypes?size=101",
        "/api/datasets?page=-1", "/api/datasets?size=0", "/api/datasets?size=101",
        "/api/datasets?page=abc", "/api/datatypes?size=abc",
        "/api/datasets?page=2147483647&size=100", "/api/datatypes?page=2147483647&size=100"})
    void invalidPaginationIsAProblemResponse(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void unsupportedHttpOperationsHaveConsistentErrorsAndAllowHeader() throws Exception {
        mvc.perform(patch("/api/datatypes")).andExpect(status().isMethodNotAllowed())
            .andExpect(header().exists("Allow"))
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(post("/api/scan").contentType(MediaType.TEXT_PLAIN).content("private text"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/api/unknown")).andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void ruleChangesImmediatelyApplyToEveryReferencingPolicy() throws Exception {
        String dt = type(1, "secret").get("id").asText();
        String first = dataset(dt);
        String second = dataset(dt);
        mvc.perform(put("/api/datatypes/" + dt).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(rule("Updated", 2, "password"))))
            .andExpect(status().isOk());
        for (String ds : List.of(first, second)) {
            assertThat(scan(ds, "secret").get("status").asText()).isEqualTo("not matched");
            var result = scan(ds, "password PASSWORD");
            assertThat(result.at("/detected_objects/0/name").asText()).isEqualTo("Updated");
            assertThat(result.at("/detected_objects/0/match_count").asInt()).isEqualTo(2);
        }
        mvc.perform(delete("/api/datasets/" + first)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/datatypes/" + dt)).andExpect(status().isConflict());
        mvc.perform(delete("/api/datasets/" + second)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/datatypes/" + dt)).andExpect(status().isNoContent());
    }

    @Test
    void failedRuleUpdatePreservesOriginalKeywordsAndThreshold() throws Exception {
        String dt = type(2, "secret").get("id").asText();
        mvc.perform(put("/api/datatypes/" + dt).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(rule("Bad", 1, "other", "OTHER"))))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/datatypes/" + dt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.threshold").value(2))
            .andExpect(jsonPath("$.content[0]").value("secret"));
    }

    @Test
    void maximumTextSizeAndRepeatedCountsAreSupported() throws Exception {
        String ds = dataset(type(16666, "secret").get("id").asText());
        String text = "secret ".repeat(14285) + "     "; // Exactly 100,000 UTF-16 code units.
        assertThat(text.length()).isEqualTo(100000);
        assertThat(scan(ds, text).get("status").asText()).isEqualTo("not matched");
        String passing = dataset(type(14285, "secret").get("id").asText());
        assertThat(scan(passing, text).at("/detected_objects/0/match_count").asInt()).isEqualTo(14285);
    }

    @Test
    void unicodeNormalizationAndRegexLiteralsWorkThroughHttp() throws Exception {
        String ds = dataset(type(3, "café", "a.b", "סודי").get("id").asText());
        var result = scan(ds, "cafe\u0301 a.b סודי aXb הסודי");
        assertThat(result.at("/detected_objects/0/match_count").asInt()).isEqualTo(3);
    }

    @Test
    void paginatedListsAreStableAndDoNotDuplicateIds() throws Exception {
        for (int i = 0; i < 4; i++) type(1, "paging" + i);
        var first = mvc.perform(get("/api/datatypes?page=0&size=2")).andExpect(status().isOk()).andReturn();
        var repeated = mvc.perform(get("/api/datatypes?page=0&size=2")).andExpect(status().isOk()).andReturn();
        var second = mvc.perform(get("/api/datatypes?page=1&size=2")).andExpect(status().isOk()).andReturn();
        assertThat(first.getResponse().getContentAsString()).isEqualTo(repeated.getResponse().getContentAsString());
        var ids = new HashSet<String>();
        for (var response : List.of(first, second)) {
            var values = json.readTree(response.getResponse().getContentAsString());
            assertThat(values.size()).isEqualTo(2);
            values.forEach(value -> assertThat(ids.add(value.get("id").asText())).isTrue());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null,\"data_type_ids\":[]}",
        "{\"name\":\"\",\"data_type_ids\":[]}", "{\"name\":\"Policy\",\"data_type_ids\":null}",
        "{\"name\":\"Policy\",\"data_type_ids\":[null]}",
        "{\"name\":\"Policy\",\"data_type_ids\":[\"invalid\"]}"})
    void invalidPolicyPayloadsAreRejected(String body) throws Exception {
        mvc.perform(post("/api/datasets").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void allUnknownResourceMutationsReturn404() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(delete("/api/datatypes/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/datasets/" + id)).andExpect(status().isNotFound());
        mvc.perform(put("/api/datatypes/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(rule("Rule", 1, "secret")))).andExpect(status().isNotFound());
        mvc.perform(put("/api/datasets/" + id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Policy\",\"data_type_ids\":[]}")).andExpect(status().isNotFound());
    }

    private Map<String, Object> rule(String name, int threshold, String... words) {
        return Map.of("name", name, "description", "Demo", "type", "keywords", "content", words, "threshold", threshold);
    }
    private JsonNode create(String path, Object body) throws Exception {
        var result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
    private JsonNode type(int threshold, String... words) throws Exception {
        return create("/api/datatypes", rule("Sensitive", threshold, words));
    }
    private String dataset(String... ids) throws Exception {
        return create("/api/datasets", Map.of("name", "Policy", "data_type_ids", ids)).get("id").asText();
    }
    private JsonNode scan(String id, String text) throws Exception {
        var result = mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("data_set_id", id, "text", text))))
            .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
    @Test
    void fullCrudAndRelationshipLifecycle() throws Exception {
        String dt = type(2, "Secret", "password").get("id").asText();
        mvc.perform(get("/api/datatypes/" + dt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0]").value("Secret"));
        mvc.perform(get("/api/datatypes")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(put("/api/datatypes/" + dt).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(rule("Updated", 1, "confidential"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        String ds = dataset(dt);
        mvc.perform(get("/api/datasets/" + ds)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data_type_ids[0]").value(dt));
        mvc.perform(get("/api/datasets")).andExpect(status().isOk());
        mvc.perform(delete("/api/datatypes/" + dt)).andExpect(status().isConflict());
        mvc.perform(put("/api/datasets/" + ds).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("name", "Empty", "data_type_ids", List.of()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data_type_ids").isEmpty());
        mvc.perform(delete("/api/datatypes/" + dt)).andExpect(status().isNoContent());
        mvc.perform(get("/api/datatypes/" + dt)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/datasets/" + ds)).andExpect(status().isNoContent());
        mvc.perform(get("/api/datasets/" + ds)).andExpect(status().isNotFound());
    }
    @Test
    void exactThresholdAndOrLogicReturnOnlyPassingRules() throws Exception {
        String first = type(3, "secret", "password").get("id").asText();
        String second = type(4, "secret").get("id").asText();
        String ds = dataset(first, second);
        var result = scan(ds, "SECRET secret password secretary");
        assertThat(result.get("status").asText()).isEqualTo("match");
        assertThat(result.get("detected_objects").size()).isEqualTo(1);
        assertThat(result.at("/detected_objects/0/id").asText()).isEqualTo(first);
        assertThat(result.at("/detected_objects/0/match_count").asInt()).isEqualTo(3);
        assertThat(scan(ds, "secret password").toString()).isEqualTo("{\"status\":\"not matched\"}");
    }
    @Test
    void allPassingRulesAreReturnedAndRulesOutsideDatasetIgnored() throws Exception {
        String first = type(1, "secret").get("id").asText();
        String second = type(1, "password").get("id").asText();
        type(1, "secret");
        var result = scan(dataset(first, second), "secret password");
        assertThat(result.get("detected_objects").size()).isEqualTo(2);
    }
    @Test
    void emptyTextAndEmptyPolicyDoNotMatch() throws Exception {
        assertThat(scan(dataset(), "secret").get("status").asText()).isEqualTo("not matched");
        assertThat(scan(dataset(type(1, "secret").get("id").asText()), "").has("detected_objects")).isFalse();
    }
    @Test
    void invalidRulesAndDuplicateNormalizedKeywordsRejected() throws Exception {
        for (Object body : List.of(rule("Bad", 0, "secret"), rule("Bad", 1), rule("Bad", 1, "Secret", " secret "),
                rule(" ", 1, "secret"), rule("Bad", 1, " "))) {
            mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
    @Test
    void missingReferencesAndDuplicateIdsAreRejected() throws Exception {
        String id = type(1, "secret").get("id").asText();
        for (var ids : List.of(List.of(UUID.randomUUID().toString()), List.of(id, id))) {
            mvc.perform(post("/api/datasets").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Bad", "data_type_ids", ids))))
                .andExpect(status().isBadRequest());
        }
    }
    @Test
    void malformedRequestsAndUnknownResourcesHaveUsefulStatusCodes() throws Exception {
        mvc.perform(get("/api/datatypes/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON).content("{broken"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("text", "secret", "data_set_id", UUID.randomUUID()))))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\":null,\"data_set_id\":null}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/datatypes?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/datasets?size=101")).andExpect(status().isBadRequest());
    }
    @Test
    void ruleKeywordsCanBeReorderedWithoutTransientUniquenessConflicts() throws Exception {
        String dt = type(1, "secret", "password").get("id").asText();
        mvc.perform(put("/api/datatypes/" + dt).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(rule("Reordered", 1, "password", "secret"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/datatypes/" + dt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0]").value("password"))
            .andExpect(jsonPath("$.content[1]").value("secret"));
    }
    @Test
    void oversizedTextUnknownFieldsAndUnsupportedRuleTypesAreRejected() throws Exception {
        mvc.perform(post("/api/scan").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("text", "a".repeat(100001), "data_set_id", UUID.randomUUID()))))
            .andExpect(status().isBadRequest());
        var body = new HashMap<>(rule("Bad", 1, "secret"));
        body.put("type", "regex");
        mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.put("type", "keywords");
        body.put("id", UUID.randomUUID());
        mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.remove("id");
        body.put("threshold", 1.5);
        mvc.perform(post("/api/datatypes").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
    }
    @Test
    void failedPolicyUpdateDoesNotOverwriteExistingState() throws Exception {
        String dt = type(1, "secret").get("id").asText();
        String ds = dataset(dt);
        mvc.perform(put("/api/datasets/" + ds).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("name", "Changed", "data_type_ids", List.of(UUID.randomUUID())))))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/datasets/" + ds)).andExpect(jsonPath("$.name").value("Policy"))
            .andExpect(jsonPath("$.data_type_ids[0]").value(dt));
    }
}
