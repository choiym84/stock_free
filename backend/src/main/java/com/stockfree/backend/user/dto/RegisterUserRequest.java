package com.stockfree.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[\\p{L}\\p{N}_]+$", message = "must contain only letters, numbers, or underscores")
        String nickname,

        @NotBlank
        @Size(min = 8, max = 64)
        String password
) {
}
