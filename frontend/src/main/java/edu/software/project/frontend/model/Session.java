package edu.software.project.frontend.model;

public record Session(
        String baseUrl,
        String token,
        UserProfile user
) {
}
