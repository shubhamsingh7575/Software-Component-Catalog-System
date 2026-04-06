package edu.software.project.backend.repository;

import edu.software.project.backend.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByToken(String token);

    void deleteByExpiresAtBefore(Instant cutoff);
}
