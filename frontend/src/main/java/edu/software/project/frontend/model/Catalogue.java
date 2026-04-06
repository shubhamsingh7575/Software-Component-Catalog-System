package edu.software.project.frontend.model;

import java.util.List;

public record Catalogue(
        long id,
        String name,
        String description,
        String keywords,
        long ownerId,
        String ownerUsername,
        List<ComponentListItem> components
) {
}
