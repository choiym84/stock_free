package com.stockfree.backend.common.exception;

import java.time.Duration;

public class PasswordResetRateLimitExceededException extends RuntimeException {

    private final Duration retryAfter;

    public PasswordResetRateLimitExceededException(Duration retryAfter) {
        super("Too many password reset requests. Try again later");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
