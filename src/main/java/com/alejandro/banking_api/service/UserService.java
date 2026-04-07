package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.UserProfileResponse;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );

    }
}
