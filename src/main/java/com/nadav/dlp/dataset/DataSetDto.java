package com.nadav.dlp.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class DataSetDto {
    private DataSetDto() {}
    /** An empty membership list is valid; duplicate or missing rule IDs are rejected by the service. */
    public record Request(
        @NotBlank @Size(max = 120) String name,
        @JsonProperty("data_type_ids") @NotNull @Size(max = 100) List<@NotNull UUID> dataTypeIds
    ) {}
    public record Response(UUID id, String name, @JsonProperty("data_type_ids") List<UUID> dataTypeIds) {
        public static Response from(DataSet entity) {
            return new Response(entity.getId(), entity.getName(), entity.getDataTypes().stream()
                .map(com.nadav.dlp.datatype.DataType::getId).sorted().toList());
        }
    }
}
