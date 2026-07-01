package com.ecommerce.identity.adapter.port.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
