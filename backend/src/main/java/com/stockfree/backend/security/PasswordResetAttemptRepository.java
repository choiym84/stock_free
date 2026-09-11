package com.stockfree.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface PasswordResetAttemptRepository extends JpaRepository<PasswordResetAttempt, Long> {

    long countByEmailAndCreatedAtAfter(String email, Instant createdAfter);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, Instant createdAfter);
}
