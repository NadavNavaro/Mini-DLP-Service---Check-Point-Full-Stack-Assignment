package com.nadav.dlp.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MatchedKeyword(String keyword, @JsonProperty("match_count") int matchCount) {}
