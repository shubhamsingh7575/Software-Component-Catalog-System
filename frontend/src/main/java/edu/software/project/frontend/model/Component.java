package edu.software.project.frontend.model;

import java.util.List;

public record Component(
        long id,
        String name,
        String description,
        String keywords,
        ComponentType type,
        long usageCount,
        long searchHitCount,
        long searchedButNotUsedCount,
        List<Long> catalogueIds
) {
}
