package com.stockfree.backend.security;

import com.stockfree.backend.common.exception.LoginRateLimitExceededException;
import com.stockfree.backend.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationAttemptService {

    private final AuthenticationEventRepository authenticationEventRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public void assertAllowed(String email, AuthenticationClient client) {
        SecurityProperties.LoginThrottle throttle = securityProperties.loginThrottle();
        Instant windowStart = clock.instant().minus(throttle.window());
        Instant emailFailureStart = authenticationEventRepository
                .findTopByAttemptedEmailAndOutcomeOrderByCreatedAtDesc(email, AuthenticationOutcome.SUCCEEDED)
                .map(AuthenticationEvent::getCreatedAt)
                .filter(lastSuccess -> lastSuccess.isAfter(windowStart))
                .orElse(windowStart);

        long emailFailures = authenticationEventRepository
                .countByAttemptedEmailAndOutcomeAndCreatedAtAfter(
                        email,
                        AuthenticationOutcome.FAILED,
                        emailFailureStart
                );
        long ipFailures = authenticationEventRepository.countByIpAddressAndOutcomeAndCreatedAtAfter(
                client.ipAddress(),
                AuthenticationOutcome.FAILED,
                windowStart
        );

        if (emailFailures >= throttle.maxFailuresPerEmail()
                || ipFailures >= throttle.maxFailuresPerIp()) {
            throw new LoginRateLimitExceededException(throttle.window());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long userId, String email, AuthenticationClient client) {
        authenticationEventRepository.save(AuthenticationEvent.create(
                userId,
                email,
                AuthenticationOutcome.SUCCEEDED,
                client
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email, AuthenticationClient client) {
        authenticationEventRepository.save(AuthenticationEvent.create(
                null,
                email,
                AuthenticationOutcome.FAILED,
                client
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRateLimited(String email, AuthenticationClient client) {
        authenticationEventRepository.save(AuthenticationEvent.create(
                null,
                email,
                AuthenticationOutcome.RATE_LIMITED,
                client
        ));
    }
}
