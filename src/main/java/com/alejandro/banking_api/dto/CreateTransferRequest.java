package com.alejandro.banking_api.dto;

import com.alejandro.banking_api.entity.Account;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransferRequest(
        @NotBlank(message = "Source account number is required")
        String sourceAccountNumber,
        @NotBlank(message = "Destination account number is required")
        String destinationAccountNumber,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.50", message = "Amount must be greater than 0.50cents")
        BigDecimal amount,
        String description

) {
}
