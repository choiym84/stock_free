package com.stockfree.backend.user.dto;

import com.stockfree.backend.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(@NotNull UserStatus status) {
}
