package com.stockfree.backend.user.service;

import com.stockfree.backend.config.NotificationProperties;
import com.stockfree.backend.user.domain.AccountTokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.delivery", havingValue = "console", matchIfMissing = true)
public class ConsoleAccountNotificationSender implements AccountNotificationSender {

    private final NotificationProperties properties;

    @Override
    public void send(String email, AccountTokenType type, String rawToken) {
        log.info("Development account notification: email={}, type={}, url={}",
                email,
                type,
                actionUrl(type, rawToken));
    }

    private String actionUrl(AccountTokenType type, String rawToken) {
        String path = type == AccountTokenType.EMAIL_VERIFICATION
                ? "/verify-email?token="
                : "/reset-password?token=";
        return properties.baseUrl() + path + rawToken;
    }
}
