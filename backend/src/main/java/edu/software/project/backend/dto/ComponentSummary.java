package edu.software.project.backend.dto;

import edu.software.project.backend.entity.ComponentType;

public record ComponentSummary(
        Long id,
        String name,
        ComponentType type
) {
}
