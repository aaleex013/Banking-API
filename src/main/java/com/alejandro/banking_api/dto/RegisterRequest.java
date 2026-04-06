package com.alejandro.banking_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message ="Email cannot be blank")
        @Email(message = "Email isn´t valid")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min=6, message = "Password need 6 characters")
        String password,

        @NotBlank(message = "Name cannot be blank")
        String fullName
) {
}
