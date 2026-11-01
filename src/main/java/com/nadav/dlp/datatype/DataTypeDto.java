package com.nadav.dlp.datatype;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class DataTypeDto {
    private DataTypeDto() {}

    public record Request(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotNull @Pattern(regexp = "keywords", message = "must be keywords") String type,
        @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 100) String> content,
        @NotNull @Min(1) Integer threshold
    ) {}

    public record Response(UUID id, String name, String description, String type,
                           List<String> content, int threshold) {
        public static Response from(DataType entity) {
            return new Response(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getType(), entity.getContent(), entity.getThreshold());
        }
    }
}
