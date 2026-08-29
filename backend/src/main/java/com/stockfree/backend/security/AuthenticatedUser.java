package com.stockfree.backend.security;

import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.domain.UserRole;
import com.stockfree.backend.user.domain.UserStatus;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public final class AuthenticatedUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String nickname;
    private String passwordHash;
    private final UserRole role;
    private final UserStatus status;
    private final boolean emailVerified;
    private final boolean emailVerificationRequired;
    private final Instant createdAt;

    private AuthenticatedUser(
            Long id,
            String email,
            String nickname,
            String passwordHash,
            UserRole role,
            UserStatus status,
            boolean emailVerified,
            boolean emailVerificationRequired,
            Instant createdAt
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.emailVerificationRequired = emailVerificationRequired;
        this.createdAt = createdAt;
    }

    public static AuthenticatedUser from(User user) {
        return from(user, false);
    }

    public static AuthenticatedUser from(User user, boolean emailVerificationRequired) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                emailVerificationRequired,
                user.getCreatedAt()
        );
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String nickname() {
        return nickname;
    }

    public UserRole role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && (!emailVerificationRequired || emailVerified);
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser[" +
                "id=" + id +
                ", email=" + email +
                ", nickname=" + nickname +
                ", role=" + role +
                ", status=" + status +
                ", emailVerified=" + emailVerified +
                ", createdAt=" + createdAt +
                ']';
    }
}
