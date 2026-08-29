package com.stockfree.backend.user.service;

import com.stockfree.backend.config.NotificationProperties;
import com.stockfree.backend.user.domain.AccountTokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.delivery", havingValue = "smtp")
public class SmtpAccountNotificationSender implements AccountNotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(String email, AccountTokenType type, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject(type == AccountTokenType.EMAIL_VERIFICATION
                ? "Verify your Stock Free email"
                : "Reset your Stock Free password");
        message.setText(messageBody(type, rawToken));
        mailSender.send(message);
    }

    private String messageBody(AccountTokenType type, String rawToken) {
        String path = type == AccountTokenType.EMAIL_VERIFICATION
                ? "/verify-email?token="
                : "/reset-password?token=";
        return "Open this link to continue: " + properties.baseUrl() + path + rawToken;
    }
}
