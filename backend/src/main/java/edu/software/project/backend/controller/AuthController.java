package edu.software.project.backend.controller;

import edu.software.project.backend.dto.AuthResponse;
import edu.software.project.backend.dto.LoginRequest;
import edu.software.project.backend.dto.RegisterRequest;
import edu.software.project.backend.security.AuthenticatedUser;
import edu.software.project.backend.security.CurrentUser;
import edu.software.project.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public void logout(
            @RequestHeader("X-Auth-Token") String token,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        authService.logout(token);
    }
}
