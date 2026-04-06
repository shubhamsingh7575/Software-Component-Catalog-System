package edu.software.project.backend.dto;

import edu.software.project.backend.entity.ComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ComponentRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 3000) String description,
        @Size(max = 1000) String keywords,
        @NotNull ComponentType type,
        Set<Long> catalogueIds
) {
}
