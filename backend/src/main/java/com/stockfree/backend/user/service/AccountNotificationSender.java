package com.stockfree.backend.user.service;

import com.stockfree.backend.user.domain.AccountTokenType;

public interface AccountNotificationSender {

    void send(String email, AccountTokenType type, String rawToken);
}
