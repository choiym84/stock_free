package com.stockfree.backend.user.dto;

import com.stockfree.backend.security.AuthenticatedUser;
import com.stockfree.backend.user.domain.User;

import java.time.Instant;

public record UserResponse(
        String email,
        String nickname,
        String role,
        String status,
        boolean emailVerified,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    public static UserResponse from(AuthenticatedUser user) {
        return new UserResponse(
                user.email(),
                user.nickname(),
                user.role().name(),
                user.status().name(),
                user.emailVerified(),
                user.createdAt()
        );
    }
}
