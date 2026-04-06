package edu.software.project.frontend.model;

public record Catalogue(
        long id,
        String name,
        String description,
        String keywords,
        long ownerId,
        String ownerUsername,
        java.util.List<Component> components
) {
}
