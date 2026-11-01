package com.nadav.dlp.datatype.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record DataTypeRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 1000) String description,
    @NotNull @Pattern(regexp = "keywords", message = "must be keywords") String type,
    @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 100) String> content,
    @NotNull @Min(1) Integer threshold
) {}
