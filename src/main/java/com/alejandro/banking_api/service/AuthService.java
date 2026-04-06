package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.AuthResponse;
import com.alejandro.banking_api.dto.LoginRequest;
import com.alejandro.banking_api.dto.LoginResponse;
import com.alejandro.banking_api.dto.RegisterRequest;
import com.alejandro.banking_api.entity.Role;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.EmailAlreadyExistsException;
import com.alejandro.banking_api.exception.InvalidCredentialsException;
import com.alejandro.banking_api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        User user = User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .fullName(registerRequest.fullName())
                .role(Role.USER)
                .active(true)
                .build();
        userRepository.save(user);
        return new AuthResponse("User registered successfully");
    }

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email")
        );
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }
        if (!user.isActive()) {
            throw new InvalidCredentialsException("User is not active");
        }
        String token = jwtService.generateToken(user.getEmail());
        return new LoginResponse(token);
    }

}
