package com.nadav.dlp.scan;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

/** Defines synchronous scan input, assignment-compatible output and opt-in inspection explanations. */
public final class ScanDto {
    private ScanDto() {}
    public record Request(@NotNull @Size(max = 100000) String text,
        @JsonProperty("data_set_id") @NotNull UUID dataSetId,
        @JsonProperty("include_details") Boolean includeDetails) {
        public Request(String text, UUID dataSetId) { this(text, dataSetId, false); }
        public boolean detailsRequested() { return Boolean.TRUE.equals(includeDetails); }
    }

    public record Detection(UUID id, String name, @JsonProperty("match_count") int matchCount) {}

    public record KeywordDetection(String keyword, @JsonProperty("match_count") int matchCount) {}

    public record RuleDetail(UUID id, String name, int threshold,
        @JsonProperty("match_count") int matchCount, boolean matched, String reason,
        @JsonProperty("matched_keywords") List<KeywordDetection> matchedKeywords) {}

    /** Diagnostic metadata for all selected rules; submitted text and surrounding excerpts are never returned. */
    public record InspectionDetails(@JsonProperty("data_set_id") UUID dataSetId,
        @JsonProperty("data_set_name") String dataSetName, String reason, List<RuleDetail> rules) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(String status, @JsonProperty("detected_objects") List<Detection> detectedObjects,
        @JsonProperty("inspection_details") InspectionDetails inspectionDetails) {
        /** Omits detected_objects entirely when no rule passes, matching the assignment response example. */
        public static Response of(List<Detection> detections, InspectionDetails details) {
            return detections.isEmpty() ? new Response("not matched", null, details)
                : new Response("match", detections, details);
        }
    }
}
