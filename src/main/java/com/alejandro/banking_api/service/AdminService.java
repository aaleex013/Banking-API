package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.AdminUserResponse;
import com.alejandro.banking_api.dto.UpdateUserStatusRequest;
import com.alejandro.banking_api.entity.Role;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToAdminUserResponse)
                .toList();
    }

    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return mapToAdminUserResponse(user);
    }

    public AdminUserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setActive(request.active());

        User updatedUser = userRepository.save(user);

        return mapToAdminUserResponse(updatedUser);
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
