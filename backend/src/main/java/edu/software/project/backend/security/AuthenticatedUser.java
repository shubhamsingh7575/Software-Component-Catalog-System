package edu.software.project.backend.security;

import edu.software.project.backend.entity.Role;

public record AuthenticatedUser(Long id, String username, String email, Role role) {
}
