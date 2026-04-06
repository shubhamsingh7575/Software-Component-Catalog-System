package edu.software.project.backend.service;

import edu.software.project.backend.config.AppProperties;
import edu.software.project.backend.entity.AuthSession;
import edu.software.project.backend.entity.User;
import edu.software.project.backend.repository.AuthSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class SessionService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthSessionRepository authSessionRepository;
    private final AppProperties appProperties;

    public SessionService(AuthSessionRepository authSessionRepository, AppProperties appProperties) {
        this.authSessionRepository = authSessionRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public String createSession(User user) {
        authSessionRepository.deleteByExpiresAtBefore(Instant.now());

        AuthSession session = new AuthSession();
        session.setToken(generateToken());
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(appProperties.getSessionTtlHours(), ChronoUnit.HOURS));
        return authSessionRepository.save(session).getToken();
    }

    @Transactional
    public void invalidateSession(String token) {
        authSessionRepository.deleteByToken(token);
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return HexFormat.of().formatHex(tokenBytes);
    }
}
