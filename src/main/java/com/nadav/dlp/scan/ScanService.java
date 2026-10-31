package com.nadav.dlp.scan;

import com.nadav.dlp.common.ApiException;
import com.nadav.dlp.dataset.DataSetRepository;
import com.nadav.dlp.datatype.DataType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Evaluates all rules in a selected policy and returns every rule meeting its own threshold. */
@Service
public class ScanService {
    private final DataSetRepository repository;
    private final KeywordMatcher matcher;
    public ScanService(DataSetRepository repository, KeywordMatcher matcher) {
        this.repository = repository;
        this.matcher = matcher;
    }

    /** Reads one consistent policy snapshot; counts are local to this scan and never persisted. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ScanDto.Response scan(ScanDto.Request request) {
        var dataSet = repository.findForScan(request.dataSetId())
            .orElseThrow(() -> ApiException.notFound("Data Set"));
        String text = matcher.normalize(request.text());
        var detections = new ArrayList<ScanDto.Detection>();
        var ruleDetails = new ArrayList<ScanDto.RuleDetail>();
        for (var type : dataSet.getDataTypes().stream().sorted(Comparator.comparing(DataType::getId)).toList()) {
            // Do not stop at the threshold: the API must report the complete occurrence count.
            var inspection = matcher.inspect(text, type.getContent(), request.detailsRequested());
            int count = inspection.count();
            boolean matched = count >= type.getThreshold();
            if (matched) {
                detections.add(new ScanDto.Detection(type.getId(), type.getName(), count));
            }
            if (request.detailsRequested()) {
                String reason = "Whole-word match count " + count + (matched ? " is at least threshold " : " is below threshold ")
                    + type.getThreshold() + ".";
                var keywords = inspection.matchedKeywords().stream()
                    .map(k -> new ScanDto.KeywordDetection(k.keyword(), k.count())).toList();
                ruleDetails.add(new ScanDto.RuleDetail(type.getId(), type.getName(), type.getThreshold(),
                    count, matched, reason, keywords));
            }
        }
        ScanDto.InspectionDetails details = null;
        if (request.detailsRequested()) {
            String reason;
            if (ruleDetails.isEmpty()) {
                reason = "The selected Data Set contains no Data Types.";
            } else if (detections.isEmpty()) {
                reason = "No Data Type reached its own threshold.";
            } else {
                reason = "At least one Data Type reached its own threshold (OR logic).";
            }
            details = new ScanDto.InspectionDetails(dataSet.getId(), dataSet.getName(), reason, List.copyOf(ruleDetails));
        }
        return ScanDto.Response.of(detections, details);
    }
}
