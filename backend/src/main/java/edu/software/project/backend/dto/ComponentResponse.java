package edu.software.project.backend.dto;

import edu.software.project.backend.entity.ComponentType;

import java.util.List;

public record ComponentResponse(
        Long id,
        String name,
        String description,
        String keywords,
        String body,
        ComponentType type,
        long usageCount,
        long searchHitCount,
        long searchedButNotUsedCount,
        Long catalogueId
) {
}
