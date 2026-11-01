package com.nadav.dlp.dataset.dto;

import com.nadav.dlp.dataset.DataSet;
import com.nadav.dlp.datatype.DataType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record DataSetResponse(UUID id, String name, @JsonProperty("data_type_ids") List<UUID> dataTypeIds) {
    public static DataSetResponse from(DataSet entity) {
        return new DataSetResponse(entity.getId(), entity.getName(), entity.getDataTypes().stream()
            .map(DataType::getId).sorted().toList());
    }
}
