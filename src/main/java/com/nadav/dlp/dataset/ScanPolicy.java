package com.nadav.dlp.dataset;

import com.nadav.dlp.datatype.InspectionRule;
import java.util.List;
import java.util.UUID;

/** A fully loaded policy snapshot that can be inspected after the database transaction closes. */
public record ScanPolicy(UUID id, String name, List<InspectionRule> rules) {
    public ScanPolicy { rules = List.copyOf(rules); }
}
