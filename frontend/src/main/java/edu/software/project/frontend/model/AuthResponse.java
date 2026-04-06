package edu.software.project.frontend.model;

public record AuthResponse(
        long userId,
        String username,
        String email,
        Role role,
        String token
) {
}
