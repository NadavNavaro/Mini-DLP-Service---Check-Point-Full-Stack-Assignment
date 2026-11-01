package com.nadav.dlp;

import com.nadav.dlp.dataset.dto.*;
import com.nadav.dlp.datatype.dto.*;
import com.nadav.dlp.scan.dto.*;

import com.nadav.dlp.datatype.*;
import com.nadav.dlp.dataset.*;
import com.nadav.dlp.scan.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;

/** Checks database-enforced relationships and independent scan results under concurrent execution. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired DataTypeService types;
    @Autowired DataSetService sets;
    @Autowired ScanService scans;
    @Autowired JdbcTemplate jdbc;

    @Test
    void databaseRejectsOrphanReferencesAndDirectDeletionOfReferencedRules() {
        var type = types.create(new DataTypeRequest("FK", "", "keywords", List.of("secret"), 1));
        var set = sets.create(new DataSetRequest("FK policy", List.of(type.id())));
        assertThatThrownBy(() -> jdbc.update("delete from data_types where id = ?", type.id()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into data_set_types(data_set_id, data_type_id) values (?, ?)",
            set.id(), java.util.UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(scans.scan(new ScanRequest("secret", set.id())).status()).isEqualTo("match");
        sets.delete(set.id());
        types.delete(type.id());
    }

    @Test
    void concurrentScansDoNotShareCountsOrState() throws Exception {
        var type = types.create(new DataTypeRequest("Concurrent", "", "keywords", List.of("secret"), 1));
        var set = sets.create(new DataSetRequest("Concurrent policy", List.of(type.id())));
        try (var executor = Executors.newFixedThreadPool(4)) {
            var futures = new ArrayList<Future<ScanResponse>>();
            for (int i = 0; i < 32; i++) {
                int repetitions = i;
                futures.add(executor.submit(() -> scans.scan(new ScanRequest("secret ".repeat(repetitions), set.id()))));
            }
            for (int i = 0; i < futures.size(); i++) {
                var result = futures.get(i).get(30, TimeUnit.SECONDS);
                if (i == 0) assertThat(result.status()).isEqualTo("not matched");
                else assertThat(result.detectedObjects().getFirst().matchCount()).isEqualTo(i);
            }
        }
        sets.delete(set.id());
        types.delete(type.id());
    }

    @Test
    void migratesRealDatabaseAndPreservesRelationshipIntegrity() {
        var type = types.create(new DataTypeRequest("Sensitive", "Demo", "keywords", List.of("secret"), 2));
        var set = sets.create(new DataSetRequest("Policy", List.of(type.id())));
        assertThat(scans.scan(new ScanRequest("secret SECRET!", set.id())).status()).isEqualTo("match");
        assertThatThrownBy(() -> types.delete(type.id())).hasMessageContaining("referenced");
        sets.delete(set.id());
        types.delete(type.id());
        assertThatThrownBy(() -> types.get(type.id())).hasMessageContaining("not found");
    }
}
