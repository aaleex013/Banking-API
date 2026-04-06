package com.alejandro.banking_api.dto;

import com.alejandro.banking_api.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType
) {
}
