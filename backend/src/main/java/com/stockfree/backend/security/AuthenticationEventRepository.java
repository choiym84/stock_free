package com.stockfree.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface AuthenticationEventRepository extends JpaRepository<AuthenticationEvent, Long> {

    long countByAttemptedEmailAndOutcomeAndCreatedAtAfter(
            String attemptedEmail,
            AuthenticationOutcome outcome,
            Instant createdAfter
    );

    long countByIpAddressAndOutcomeAndCreatedAtAfter(
            String ipAddress,
            AuthenticationOutcome outcome,
            Instant createdAfter
    );

    Optional<AuthenticationEvent> findTopByAttemptedEmailAndOutcomeOrderByCreatedAtDesc(
            String attemptedEmail,
            AuthenticationOutcome outcome
    );

    Page<AuthenticationEvent> findByAttemptedEmailOrderByCreatedAtDesc(
            String attemptedEmail,
            Pageable pageable
    );

    Page<AuthenticationEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
