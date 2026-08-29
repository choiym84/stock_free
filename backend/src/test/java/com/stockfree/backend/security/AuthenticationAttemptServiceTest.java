package com.stockfree.backend.security;

import com.stockfree.backend.common.exception.LoginRateLimitExceededException;
import com.stockfree.backend.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationAttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T00:00:00Z");

    @Mock
    private AuthenticationEventRepository authenticationEventRepository;

    private AuthenticationAttemptService authenticationAttemptService;

    @BeforeEach
    void setUp() {
        var properties = new SecurityProperties(
                List.of("http://localhost:5173"),
                new SecurityProperties.LoginThrottle(5, 20, Duration.ofMinutes(15))
        );
        authenticationAttemptService = new AuthenticationAttemptService(
                authenticationEventRepository,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void assertAllowed_belowLimits_shouldAllowAuthentication() {
        var client = new AuthenticationClient("127.0.0.1", "test-agent");
        Instant windowStart = NOW.minus(Duration.ofMinutes(15));
        when(authenticationEventRepository.findTopByAttemptedEmailAndOutcomeOrderByCreatedAtDesc(
                "user@example.com",
                AuthenticationOutcome.SUCCEEDED
        )).thenReturn(Optional.empty());
        when(authenticationEventRepository.countByAttemptedEmailAndOutcomeAndCreatedAtAfter(
                "user@example.com",
                AuthenticationOutcome.FAILED,
                windowStart
        )).thenReturn(4L);
        when(authenticationEventRepository.countByIpAddressAndOutcomeAndCreatedAtAfter(
                "127.0.0.1",
                AuthenticationOutcome.FAILED,
                windowStart
        )).thenReturn(19L);

        assertThatCode(() -> authenticationAttemptService.assertAllowed("user@example.com", client))
                .doesNotThrowAnyException();
    }

    @Test
    void assertAllowed_atEmailLimit_shouldRejectAuthentication() {
        var client = new AuthenticationClient("127.0.0.1", null);
        Instant windowStart = NOW.minus(Duration.ofMinutes(15));
        when(authenticationEventRepository.findTopByAttemptedEmailAndOutcomeOrderByCreatedAtDesc(
                "user@example.com",
                AuthenticationOutcome.SUCCEEDED
        )).thenReturn(Optional.empty());
        when(authenticationEventRepository.countByAttemptedEmailAndOutcomeAndCreatedAtAfter(
                "user@example.com",
                AuthenticationOutcome.FAILED,
                windowStart
        )).thenReturn(5L);
        when(authenticationEventRepository.countByIpAddressAndOutcomeAndCreatedAtAfter(
                "127.0.0.1",
                AuthenticationOutcome.FAILED,
                windowStart
        )).thenReturn(0L);

        assertThatThrownBy(() -> authenticationAttemptService.assertAllowed("user@example.com", client))
                .isInstanceOf(LoginRateLimitExceededException.class)
                .extracting("retryAfter")
                .isEqualTo(Duration.ofMinutes(15));
    }
}
