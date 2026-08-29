package com.stockfree.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        @Size(max = 64)
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 64)
        String newPassword
) {
}
