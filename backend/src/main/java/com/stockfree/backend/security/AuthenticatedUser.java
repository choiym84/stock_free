package com.stockfree.backend.security;

import com.stockfree.backend.user.domain.User;
import com.stockfree.backend.user.domain.UserRole;
import com.stockfree.backend.user.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        Long id,
        String email,
        String nickname,
        String passwordHash,
        UserRole role,
        UserStatus status,
        Instant createdAt
) implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
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
        return status == UserStatus.ACTIVE;
    }
}
