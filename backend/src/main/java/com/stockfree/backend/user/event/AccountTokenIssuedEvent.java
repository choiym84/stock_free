package com.stockfree.backend.user.event;

import com.stockfree.backend.user.domain.AccountTokenType;

public record AccountTokenIssuedEvent(
        String email,
        AccountTokenType type,
        String rawToken
) {
}
