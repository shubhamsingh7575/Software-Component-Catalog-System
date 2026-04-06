package edu.software.project.backend.dto;

public record CatalogueResponse(
        Long id,
        String name,
        String description,
        String keywords,
        Long ownerId,
        String ownerUsername,
        java.util.List<ComponentResponse> components
) {
}
