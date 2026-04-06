package com.alejandro.banking_api.dto;

import com.alejandro.banking_api.entity.Role;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean active,
        LocalDateTime createdAt

) {
}
