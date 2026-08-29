package com.stockfree.backend.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "authentication_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthenticationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "attempted_email", nullable = false, length = 255, updatable = false)
    private String attemptedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AuthenticationOutcome outcome;

    @Column(name = "ip_address", nullable = false, length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 255, updatable = false)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AuthenticationEvent create(
            Long userId,
            String attemptedEmail,
            AuthenticationOutcome outcome,
            AuthenticationClient client
    ) {
        AuthenticationEvent event = new AuthenticationEvent();
        event.userId = userId;
        event.attemptedEmail = attemptedEmail;
        event.outcome = outcome;
        event.ipAddress = client.ipAddress();
        event.userAgent = client.userAgent();
        return event;
    }
}
