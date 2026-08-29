package com.stockfree.backend.user.service;

import com.stockfree.backend.user.event.AccountTokenIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountNotificationListener {

    private final AccountNotificationSender notificationSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(AccountTokenIssuedEvent event) {
        try {
            notificationSender.send(event.email(), event.type(), event.rawToken());
        } catch (RuntimeException exception) {
            log.error("Failed to deliver {} notification to {}", event.type(), event.email(), exception);
        }
    }
}
