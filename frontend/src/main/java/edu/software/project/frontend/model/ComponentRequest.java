package edu.software.project.frontend.model;

import java.util.List;

public record ComponentRequest(
        String name,
        String description,
        String keywords,
        ComponentType type,
        List<Long> catalogueIds
) {
}
