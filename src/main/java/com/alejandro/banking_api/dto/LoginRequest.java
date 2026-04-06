package com.alejandro.banking_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "email cannot blank")
        @Email(message = "email is not valid")
        String email,
        @NotBlank(message = "Password cannot blank")
        String password
) {
}
