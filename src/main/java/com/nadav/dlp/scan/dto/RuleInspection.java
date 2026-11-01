package com.nadav.dlp.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record RuleInspection(UUID id, String name, int threshold,
    @JsonProperty("match_count") int matchCount, boolean matched, String reason,
    @JsonProperty("matched_keywords") List<MatchedKeyword> matchedKeywords) {}
