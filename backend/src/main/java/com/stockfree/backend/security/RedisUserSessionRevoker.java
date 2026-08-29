package com.stockfree.backend.security;

import com.stockfree.backend.user.event.UserAccessChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("redis-session")
@RequiredArgsConstructor
public class RedisUserSessionRevoker {

    private final RedisIndexedSessionRepository sessionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void revokeAfterAccessChange(UserAccessChangedEvent event) {
        sessionRepository.findByPrincipalName(event.email())
                .keySet()
                .forEach(sessionRepository::deleteById);
    }
}
