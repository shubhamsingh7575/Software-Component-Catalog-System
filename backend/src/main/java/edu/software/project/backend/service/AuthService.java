package edu.software.project.backend.service;

import edu.software.project.backend.dto.AuthResponse;
import edu.software.project.backend.dto.LoginRequest;
import edu.software.project.backend.dto.RegisterRequest;
import edu.software.project.backend.entity.Role;
import edu.software.project.backend.entity.User;
import edu.software.project.backend.exception.ApiException;
import edu.software.project.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedUsername = normalizeUsername(request.username());

        if (normalizedUsername.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username must not be blank");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password and confirm password must match");
        }
        if (!isStrongPassword(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain upper, lower, digit, and special characters");
        }
        if (userRepository.countByEmailIgnoreCase(normalizedEmail) > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Sign in instead or use a different email address.");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(userRepository.count() == 0 ? Role.ADMIN : Role.USER);

        User savedUser = userRepository.save(user);
        String token = sessionService.createSession(savedUser);
        return toAuthResponse(savedUser, token);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        List<User> matchingUsers = userRepository.findAllByEmailIgnoreCase(normalizeEmail(request.email()));
        if (matchingUsers.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (matchingUsers.size() > 1) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This email address is linked to multiple accounts in the database. Remove the duplicates and try again.");
        }
        User user = matchingUsers.get(0);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = sessionService.createSession(user);
        return toAuthResponse(user, token);
    }

    @Transactional
    public void logout(String token) {
        sessionService.invalidateSession(token);
    }

    private boolean isStrongPassword(String password) {
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char current : password.toCharArray()) {
            if (Character.isUpperCase(current)) {
                hasUpper = true;
            } else if (Character.isLowerCase(current)) {
                hasLower = true;
            } else if (Character.isDigit(current)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        return password.length() >= 8 && hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), token);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }
}
