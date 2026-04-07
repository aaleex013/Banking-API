package com.alejandro.banking_api.dto;

import com.alejandro.banking_api.entity.Account;
import com.alejandro.banking_api.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String sourceFullName,
        BigDecimal amount,
        String description,
        TransferStatus status,
        LocalDateTime createdAt
) {
}
