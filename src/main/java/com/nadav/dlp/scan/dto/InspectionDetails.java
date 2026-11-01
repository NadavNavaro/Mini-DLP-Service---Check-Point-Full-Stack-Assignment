package com.nadav.dlp.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record InspectionDetails(@JsonProperty("data_set_id") UUID dataSetId,
    @JsonProperty("data_set_name") String dataSetName, String reason, List<RuleInspection> rules) {}
