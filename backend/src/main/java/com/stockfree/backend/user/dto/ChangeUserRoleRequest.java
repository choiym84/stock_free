package com.stockfree.backend.user.dto;

import com.stockfree.backend.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(@NotNull UserRole role) {
}
