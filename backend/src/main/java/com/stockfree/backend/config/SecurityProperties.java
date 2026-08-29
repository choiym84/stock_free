package com.stockfree.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("app.security")
public record SecurityProperties(
        List<String> allowedOrigins,
        LoginThrottle loginThrottle,
        boolean requireEmailVerification,
        AccountTokens accountTokens
) {

    public SecurityProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    public record LoginThrottle(
            int maxFailuresPerEmail,
            int maxFailuresPerIp,
            Duration window
    ) {
    }

    public record AccountTokens(
            Duration emailVerificationTtl,
            Duration passwordResetTtl
    ) {
    }
}
