package com.stockfree.backend.security;

import com.stockfree.backend.common.exception.PasswordResetRateLimitExceededException;
import com.stockfree.backend.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetAttemptService {

    private final PasswordResetAttemptRepository attemptRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    @Transactional
    public void assertAllowedAndRecord(String email, String ipAddress) {
        SecurityProperties.PasswordResetThrottle throttle = securityProperties.passwordResetThrottle();
        Instant windowStart = clock.instant().minus(throttle.window());
        if (attemptRepository.countByEmailAndCreatedAtAfter(email, windowStart) >= throttle.maxRequestsPerEmail()
                || attemptRepository.countByIpAddressAndCreatedAtAfter(ipAddress, windowStart)
                >= throttle.maxRequestsPerIp()) {
            throw new PasswordResetRateLimitExceededException(throttle.window());
        }
        attemptRepository.save(PasswordResetAttempt.create(email, ipAddress));
    }
}
