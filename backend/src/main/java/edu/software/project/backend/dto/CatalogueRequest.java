package edu.software.project.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogueRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        @Size(max = 1000) String keywords
) {
}
