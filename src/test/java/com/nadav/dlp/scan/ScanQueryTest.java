package com.nadav.dlp.scan;

import com.nadav.dlp.dataset.dto.*;
import com.nadav.dlp.datatype.dto.*;
import com.nadav.dlp.scan.dto.*;

import com.nadav.dlp.datatype.*;
import com.nadav.dlp.dataset.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Guards against N+1 scan queries and duplicate counts introduced by collection fetch joins. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class ScanQueryTest {
    @Autowired DataTypeService types;
    @Autowired DataSetService sets;
    @Autowired ScanService scans;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void scanPolicyIsImmutableAndIndependentOfLaterRuleUpdates() {
        var rule = types.create(new DataTypeRequest("Snapshot", "", "keywords", List.of("secret"), 1));
        var policy = sets.create(new DataSetRequest("Snapshot policy", List.of(rule.id())));
        var snapshot = sets.loadForScan(policy.id());
        types.update(rule.id(), new DataTypeRequest("Updated", "", "keywords", List.of("token"), 2));
        assertThat(snapshot.rules().getFirst().keywords()).containsExactly("secret");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> snapshot.rules().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> snapshot.rules().getFirst().keywords().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void loadsPolicyRulesAndKeywordsWithOneSelectWithoutDuplicateCounts() {
        var ids = new ArrayList<UUID>();
        for (int i = 0; i < 20; i++) {
            ids.add(types.create(new DataTypeRequest("Rule " + i, "", "keywords",
                List.of("secret", "password", "term" + i), 2)).id());
        }
        var policy = sets.create(new DataSetRequest("Query test", ids));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = scans.scan(new ScanRequest("secret password", policy.id()));
        assertThat(result.detectedObjects()).hasSize(20);
        assertThat(result.detectedObjects()).allSatisfy(d -> assertThat(d.matchCount()).isEqualTo(2));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        statistics.clear();
        var detailed = scans.scan(new ScanRequest("secret password", policy.id(), true));
        assertThat(detailed.inspectionDetails().rules()).hasSize(20);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
