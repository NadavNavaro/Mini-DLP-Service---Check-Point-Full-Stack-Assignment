package com.nadav.dlp.dataset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record DataSetRequest(
    @NotBlank @Size(max = 120) String name,
    @JsonProperty("data_type_ids") @NotNull @Size(max = 100) List<@NotNull UUID> dataTypeIds
) {}
