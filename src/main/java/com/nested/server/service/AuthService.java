package com.nested.server.service;

import com.nested.server.dto.AuthRequest;
import com.nested.server.dto.AuthResponse;
import com.nested.server.dto.RegisterRequest;
import com.nested.server.exception.BadRequestException;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.model.User;
import com.nested.server.repository.UserRepository;
import com.nested.server.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TwoFactorService twoFactorService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for username: {}, email: {}", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username '{}' already taken", request.getUsername());
            throw new BadRequestException("Username already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email '{}' already registered", request.getEmail());
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .karma(1)
                .build();

        log.info("Saving new user to database...");
        user = userRepository.save(user);
        log.info("User saved successfully with ID: {}, username: {}", user.getId(), user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .message("Registration successful")
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        // First check if user exists
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        // Check if account is enabled
        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }

        // Check if 2FA is enabled
        if (user.isTwoFactorEnabled()) {
            // If no 2FA code provided, return response indicating 2FA is required
            if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
                log.info("2FA required for user: {}", user.getUsername());
                return AuthResponse.builder()
                        .requiresTwoFactor(true)
                        .username(user.getUsername())
                        .message("2FA verification required")
                        .build();
            }

            // Verify 2FA code
            if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                throw new BadRequestException("Invalid 2FA code");
            }
        }

        log.info("User logged in: {}", user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .message("Login successful")
                .build();
    }
}
