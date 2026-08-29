package com.stockfree.backend.user.dto;

import com.stockfree.backend.security.AuthenticationEvent;

import java.time.Instant;

public record AuthenticationEventResponse(
        String outcome,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {

    public static AuthenticationEventResponse from(AuthenticationEvent event) {
        return new AuthenticationEventResponse(
                event.getOutcome().name(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getCreatedAt()
        );
    }
}
