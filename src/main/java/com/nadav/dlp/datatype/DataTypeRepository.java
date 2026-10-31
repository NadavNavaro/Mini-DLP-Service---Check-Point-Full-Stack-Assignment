package com.nadav.dlp.datatype;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/** Provides persistence operations for rules; business constraints belong in the service. */
public interface DataTypeRepository extends JpaRepository<DataType, UUID> {}
