package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.UserProfileResponse;
import com.alejandro.banking_api.entity.Role;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final String USER_EMAIL = "alex@test.com";
    private static final String FULL_NAME = "Alejandro Hernandez";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 4, 7, 18, 30);

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldGetUserProfileSuccessfully() {
        User user = buildUser();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(USER_EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo(USER_EMAIL);
        assertThat(response.fullName()).isEqualTo(FULL_NAME);
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(USER_EMAIL))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email(USER_EMAIL)
                .password("encoded-password")
                .fullName(FULL_NAME)
                .role(Role.USER)
                .active(true)
                .createdAt(CREATED_AT)
                .build();
    }
}
