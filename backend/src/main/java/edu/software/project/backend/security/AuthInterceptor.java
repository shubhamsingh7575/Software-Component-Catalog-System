package edu.software.project.backend.security;

import edu.software.project.backend.entity.AuthSession;
import edu.software.project.backend.exception.ApiException;
import edu.software.project.backend.repository.AuthSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";
    private static final String AUTH_HEADER = "X-Auth-Token";

    private final AuthSessionRepository authSessionRepository;

    public AuthInterceptor(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(AUTH_HEADER);
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing X-Auth-Token header");
        }

        AuthSession session = authSessionRepository.findByToken(token)
                .filter(it -> it.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired session"));

        request.setAttribute(AUTHENTICATED_USER_ATTR, new AuthenticatedUser(session.getUser()));
        return true;
    }
}
