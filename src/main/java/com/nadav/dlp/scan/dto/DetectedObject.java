package com.nadav.dlp.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record DetectedObject(UUID id, String name, @JsonProperty("match_count") int matchCount) {}
