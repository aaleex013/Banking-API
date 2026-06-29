package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.AdminUserResponse;
import com.alejandro.banking_api.dto.UpdateUserStatusRequest;
import com.alejandro.banking_api.entity.Role;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.UserNotFoundException;
import com.alejandro.banking_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 29, 11, 30);

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldGetAllUsersSuccessfully() {
        User user = buildUser(
                1L,
                "user@test.com",
                "Normal User",
                Role.USER,
                true
        );

        User admin = buildUser(
                2L,
                "admin@test.com",
                "Admin User",
                Role.ADMIN,
                true
        );

        when(userRepository.findAll()).thenReturn(List.of(user, admin));

        List<AdminUserResponse> response = adminService.getAllUsers();

        assertThat(response).hasSize(2);

        assertThat(response.get(0).id()).isEqualTo(1L);
        assertThat(response.get(0).email()).isEqualTo("user@test.com");
        assertThat(response.get(0).fullName()).isEqualTo("Normal User");
        assertThat(response.get(0).role()).isEqualTo(Role.USER);
        assertThat(response.get(0).active()).isTrue();
        assertThat(response.get(0).createdAt()).isEqualTo(CREATED_AT);

        assertThat(response.get(1).id()).isEqualTo(2L);
        assertThat(response.get(1).email()).isEqualTo("admin@test.com");
        assertThat(response.get(1).fullName()).isEqualTo("Admin User");
        assertThat(response.get(1).role()).isEqualTo(Role.ADMIN);
        assertThat(response.get(1).active()).isTrue();
        assertThat(response.get(1).createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        User user = buildUser(
                1L,
                "user@test.com",
                "Normal User",
                Role.USER,
                true
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AdminUserResponse response = adminService.getUserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.fullName()).isEqualTo("Normal User");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowWhenUserNotFoundGettingUserById() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void shouldUpdateUserStatusSuccessfully() {
        User user = buildUser(
                1L,
                "user@test.com",
                "Normal User",
                Role.USER,
                true
        );

        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminService.updateUserStatus(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.fullName()).isEqualTo("Normal User");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.active()).isFalse();
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(userRepository, times(1)).findById(1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(1L);
        assertThat(savedUser.isActive()).isFalse();
    }

    @Test
    void shouldThrowWhenUserNotFoundUpdatingStatus() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(false);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserStatus(99L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    private User buildUser(
            Long id,
            String email,
            String fullName,
            Role role,
            boolean active
    ) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName(fullName)
                .password("encoded-password")
                .role(role)
                .active(active)
                .createdAt(CREATED_AT)
                .build();
    }
}
