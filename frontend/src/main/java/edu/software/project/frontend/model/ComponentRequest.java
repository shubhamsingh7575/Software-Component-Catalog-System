package edu.software.project.frontend.model;

public record ComponentRequest(
        String name,
        String description,
        String keywords,
        String body,
        ComponentType type
) {
}
