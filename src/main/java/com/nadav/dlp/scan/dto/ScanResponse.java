package com.nadav.dlp.scan.dto;

import com.fasterxml.jackson.annotation.*;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScanResponse(String status, @JsonProperty("detected_objects") List<DetectedObject> detectedObjects,
    @JsonProperty("inspection_details") InspectionDetails inspectionDetails) {
    public static ScanResponse of(List<DetectedObject> detections, InspectionDetails details) {
        return detections.isEmpty() ? new ScanResponse("not matched", null, details)
            : new ScanResponse("match", detections, details);
    }
}
