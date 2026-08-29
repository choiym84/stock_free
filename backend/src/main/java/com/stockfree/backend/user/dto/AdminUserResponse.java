package com.stockfree.backend.user.dto;

import com.stockfree.backend.user.domain.User;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
