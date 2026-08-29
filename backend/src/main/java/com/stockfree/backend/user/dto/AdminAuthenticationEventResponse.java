package com.stockfree.backend.user.dto;

import com.stockfree.backend.security.AuthenticationEvent;

import java.time.Instant;

public record AdminAuthenticationEventResponse(
        Long id,
        Long userId,
        String attemptedEmail,
        String outcome,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {

    public static AdminAuthenticationEventResponse from(AuthenticationEvent event) {
        return new AdminAuthenticationEventResponse(
                event.getId(),
                event.getUserId(),
                event.getAttemptedEmail(),
                event.getOutcome().name(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getCreatedAt()
        );
    }
}
