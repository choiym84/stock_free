package com.stockfree.backend.security;

import com.stockfree.backend.user.event.UserAccessChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserSessionRevoker {

    private final SessionRegistry sessionRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void revokeAfterAccessChange(UserAccessChangedEvent event) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast)
                .filter(principal -> principal.id().equals(event.userId()))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(sessionInformation -> sessionInformation.expireNow());
    }
}
