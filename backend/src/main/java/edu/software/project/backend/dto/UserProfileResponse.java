package edu.software.project.backend.dto;

import edu.software.project.backend.entity.Role;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
