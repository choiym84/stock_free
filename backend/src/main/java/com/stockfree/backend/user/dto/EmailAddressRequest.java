package com.stockfree.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailAddressRequest(
        @NotBlank @Email @Size(max = 255) String email
) {
}
