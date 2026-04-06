package edu.software.project.backend.dto;

import java.util.List;

public record CatalogueResponse(
        Long id,
        String name,
        String description,
        String keywords,
        Long ownerId,
        String ownerUsername,
        List<ComponentSummary> components
) {
}
