package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.AuthResponse;
import com.alejandro.banking_api.dto.LoginRequest;
import com.alejandro.banking_api.dto.LoginResponse;
import com.alejandro.banking_api.dto.RegisterRequest;
import com.alejandro.banking_api.entity.Role;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.EmailAlreadyExistsException;
import com.alejandro.banking_api.exception.InactiveUserException;
import com.alejandro.banking_api.exception.InvalidCredentialsException;
import com.alejandro.banking_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    public static final String USER_EMAIL = "alex22escribano@gmail.com";
    public static final String PASSWORD = "password";
    public static final String ENCODED_PASSWORD = "encoded-password";
    public static final String FULL_NAME = "Alejandro Hernandez";
    public static final String JWT_TOKEN = "jwt-token";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                USER_EMAIL, PASSWORD, FULL_NAME
        );
        when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);

        AuthResponse authResponse = authService.register(request);
        assertThat(authResponse).isEqualTo(new AuthResponse("User registered successfully"));

        verify(userRepository, times(1)).existsByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).encode(PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        User userSaved = captor.getValue();

        assertThat(userSaved.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(userSaved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(userSaved.getFullName()).isEqualTo(FULL_NAME);
        assertThat(userSaved.getRole()).isEqualTo(Role.USER);
        assertThat(userSaved.isActive()).isTrue();

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                USER_EMAIL, PASSWORD, FULL_NAME
        );
        when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(true);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already exists");

        verify(userRepository, times(1)).existsByEmail(USER_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldLoginUserSuccessfully() {
        LoginRequest request = new LoginRequest(
                USER_EMAIL, PASSWORD
        );
        User user = buildUser(true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(USER_EMAIL)).thenReturn(JWT_TOKEN);

        LoginResponse loginResponse = authService.login(request);
        assertThat(loginResponse.token()).isEqualTo(JWT_TOKEN);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(PASSWORD, ENCODED_PASSWORD);
        verify(jwtService, times(1)).generateToken(USER_EMAIL);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnLogin() {
        LoginRequest request = new LoginRequest(
                USER_EMAIL, PASSWORD
        );

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or user not found");

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldThrowWhenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest(
                USER_EMAIL, PASSWORD
        );
        User user = buildUser(true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid password");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(PASSWORD, ENCODED_PASSWORD);
        verifyNoInteractions(jwtService);
    }
    @Test
    void shouldThrowWhenUserIsNotActive() {
        LoginRequest request = new LoginRequest(
                USER_EMAIL, PASSWORD
        );
        User user = buildUser(false);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(InactiveUserException.class)
                .hasMessage("User is not active");
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(PASSWORD, ENCODED_PASSWORD);
        verifyNoInteractions(jwtService);

    }

    private User buildUser(boolean active) {
        return User.builder()
                .id(1L)
                .email(USER_EMAIL)
                .password(ENCODED_PASSWORD)
                .fullName(FULL_NAME)
                .role(Role.USER)
                .active(active)
                .build();
    }
}
