package edu.software.project.frontend.model;

public record Component(
        long id,
        String name,
        String description,
        String keywords,
        String body,
        ComponentType type,
        long usageCount,
        long searchHitCount,
        long searchedButNotUsedCount,
        long catalogueId
) {
}
