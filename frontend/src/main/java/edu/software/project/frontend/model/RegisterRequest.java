package edu.software.project.frontend.model;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String confirmPassword
) {
}
