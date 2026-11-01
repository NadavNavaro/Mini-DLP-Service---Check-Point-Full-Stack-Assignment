package com.nadav.dlp.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ScanRequest(@NotNull @Size(max = 100000) String text,
    @JsonProperty("data_set_id") @NotNull UUID dataSetId,
    @JsonProperty("include_details") Boolean includeDetails) {
    public ScanRequest(String text, UUID dataSetId) { this(text, dataSetId, false); }
    public boolean detailsRequested() { return Boolean.TRUE.equals(includeDetails); }
}
