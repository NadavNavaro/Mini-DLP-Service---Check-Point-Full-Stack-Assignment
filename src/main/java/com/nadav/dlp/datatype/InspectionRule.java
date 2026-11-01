package com.nadav.dlp.datatype;

import java.util.List;
import java.util.UUID;

/** Immutable scan data, independent of JPA and the HTTP response. */
public record InspectionRule(UUID id, String name, List<String> keywords, int threshold) {
    public InspectionRule { keywords = List.copyOf(keywords); }
}
