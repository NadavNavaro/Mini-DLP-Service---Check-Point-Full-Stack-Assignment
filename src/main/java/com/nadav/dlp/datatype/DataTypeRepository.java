package com.nadav.dlp.datatype;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DataTypeRepository extends JpaRepository<DataType, UUID> {}
