package edu.software.project.backend.dto;

import edu.software.project.backend.entity.Role;

public record AuthResponse(
        Long userId,
        String username,
        String email,
        Role role,
        String token
) {
}
