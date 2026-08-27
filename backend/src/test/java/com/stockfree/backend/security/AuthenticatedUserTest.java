package com.stockfree.backend.security;

import com.stockfree.backend.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void eraseCredentials_afterAuthentication_shouldRemovePasswordHash() {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(
                User.register("user@example.com", "investor_1", "encoded-password")
        );

        assertThat(authenticatedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(authenticatedUser.toString()).doesNotContain("encoded-password", "passwordHash");

        authenticatedUser.eraseCredentials();

        assertThat(authenticatedUser.getPassword()).isNull();
    }
}
