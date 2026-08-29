package com.stockfree.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[\\p{L}\\p{N}_]+$", message = "must contain only letters, numbers, or underscores")
        String nickname
) {
}
