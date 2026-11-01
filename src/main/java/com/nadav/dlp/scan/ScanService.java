package com.nadav.dlp.scan;

import com.nadav.dlp.scan.dto.*;

import com.nadav.dlp.dataset.DataSetService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/** Evaluates all rules in a selected policy and returns every rule meeting its own threshold. */
@Service
public class ScanService {
    private final DataSetService policies;
    private final KeywordMatcher matcher;
    public ScanService(DataSetService policies, KeywordMatcher matcher) {
        this.policies = policies;
        this.matcher = matcher;
    }

    public ScanResponse scan(ScanRequest request) {
        var dataSet = policies.loadForScan(request.dataSetId());
        String text = matcher.normalize(request.text());
        var detections = new ArrayList<DetectedObject>();
        var ruleDetails = new ArrayList<RuleInspection>();
        for (var type : dataSet.rules()) {
            // Do not stop at the threshold: the API must report the complete occurrence count.
            var inspection = matcher.inspect(text, type.keywords(), request.detailsRequested());
            int count = inspection.count();
            boolean matched = count >= type.threshold();
            if (matched) {
                detections.add(new DetectedObject(type.id(), type.name(), count));
            }
            if (request.detailsRequested()) {
                String reason = "Whole-word match count " + count + (matched ? " is at least threshold " : " is below threshold ")
                    + type.threshold() + ".";
                var keywords = inspection.matchedKeywords().stream()
                    .map(k -> new MatchedKeyword(k.keyword(), k.count())).toList();
                ruleDetails.add(new RuleInspection(type.id(), type.name(), type.threshold(),
                    count, matched, reason, keywords));
            }
        }
        InspectionDetails details = null;
        if (request.detailsRequested()) {
            String reason;
            if (ruleDetails.isEmpty()) {
                reason = "The selected Data Set contains no Data Types.";
            } else if (detections.isEmpty()) {
                reason = "No Data Type reached its own threshold.";
            } else {
                reason = "At least one Data Type reached its own threshold (OR logic).";
            }
            details = new InspectionDetails(dataSet.id(), dataSet.name(), reason, List.copyOf(ruleDetails));
        }
        return ScanResponse.of(detections, details);
    }
}
