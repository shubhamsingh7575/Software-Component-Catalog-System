package edu.software.project.backend.repository;

import edu.software.project.backend.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<AuthSession> findByToken(String token);

    @EntityGraph(attributePaths = "user")
    Optional<AuthSession> findByTokenAndExpiresAtAfter(String token, Instant cutoff);

    void deleteByExpiresAtBefore(Instant cutoff);

    void deleteByToken(String token);
}
