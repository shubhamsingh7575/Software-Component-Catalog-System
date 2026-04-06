package edu.software.project.frontend.model;

public record LoginRequest(
        String email,
        String password
) {
}
