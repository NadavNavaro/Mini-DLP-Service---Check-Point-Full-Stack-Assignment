package com.nadav.dlp.dataset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface DataSetRepository extends JpaRepository<DataSet, UUID> {
    boolean existsByDataTypes_Id(UUID id);

    // Fetch only for scans; collection fetch joins must not be applied to paginated lists.
    @Query("select distinct ds from DataSet ds left join fetch ds.dataTypes dt "
        + "left join fetch dt.content where ds.id = :id")
    Optional<DataSet> findForScan(@Param("id") UUID id);
}
