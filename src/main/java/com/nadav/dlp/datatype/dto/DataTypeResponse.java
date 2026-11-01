package com.nadav.dlp.datatype.dto;

import com.nadav.dlp.datatype.DataType;
import java.util.List;
import java.util.UUID;

public record DataTypeResponse(UUID id, String name, String description, String type,
                              List<String> content, int threshold) {
    public static DataTypeResponse from(DataType entity) {
        return new DataTypeResponse(entity.getId(), entity.getName(), entity.getDescription(),
            entity.getType(), entity.getContent(), entity.getThreshold());
    }
}
