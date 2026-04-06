package edu.software.project.frontend.model;

public record UserProfile(
        long id,
        String username,
        String email,
        Role role
) {
}
