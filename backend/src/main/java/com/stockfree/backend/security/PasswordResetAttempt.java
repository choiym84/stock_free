package com.stockfree.backend.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "password_reset_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private PasswordResetAttempt(String email, String ipAddress) {
        this.email = email;
        this.ipAddress = ipAddress;
    }

    public static PasswordResetAttempt create(String email, String ipAddress) {
        return new PasswordResetAttempt(email, ipAddress);
    }
}
