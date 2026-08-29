package com.stockfree.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawUserRequest(
        @NotBlank
        @Size(max = 64)
        String currentPassword
) {
}
