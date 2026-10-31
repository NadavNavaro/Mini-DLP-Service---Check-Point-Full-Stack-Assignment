package com.nadav.dlp.scan;

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
    void loadsPolicyRulesAndKeywordsWithOneSelectWithoutDuplicateCounts() {
        var ids = new ArrayList<UUID>();
        for (int i = 0; i < 20; i++) {
            ids.add(types.create(new DataTypeDto.Request("Rule " + i, "", "keywords",
                List.of("secret", "password", "term" + i), 2)).id());
        }
        var policy = sets.create(new DataSetDto.Request("Query test", ids));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = scans.scan(new ScanDto.Request("secret password", policy.id()));
        assertThat(result.detectedObjects()).hasSize(20);
        assertThat(result.detectedObjects()).allSatisfy(d -> assertThat(d.matchCount()).isEqualTo(2));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        statistics.clear();
        var detailed = scans.scan(new ScanDto.Request("secret password", policy.id(), true));
        assertThat(detailed.inspectionDetails().rules()).hasSize(20);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
