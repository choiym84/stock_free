package com.stockfree.backend.security;

import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.event.UserAccessChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionRevokerTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private SessionInformation sessionInformation;

    @InjectMocks
    private UserSessionRevoker userSessionRevoker;

    @Test
    void revokeAfterAccessChange_withMatchingUser_shouldExpireAllActiveSessions() {
        AuthenticatedUser matchingUser = authenticatedUser(1L, "matching@example.com");
        AuthenticatedUser otherUser = authenticatedUser(2L, "other@example.com");
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(matchingUser, otherUser));
        when(sessionRegistry.getAllSessions(matchingUser, false)).thenReturn(List.of(sessionInformation));

        userSessionRevoker.revokeAfterAccessChange(new UserAccessChangedEvent(1L));

        verify(sessionInformation).expireNow();
        verify(sessionRegistry, never()).getAllSessions(otherUser, false);
    }

    private AuthenticatedUser authenticatedUser(Long id, String email) {
        User user = User.register(email, "nickname_" + id, "encoded-password");
        setId(user, id);
        return AuthenticatedUser.from(user);
    }

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
