package com.thehartford.identityservice.service;

import com.thehartford.identityservice.dto.*;
import com.thehartford.identityservice.model.User;
import com.thehartford.identityservice.model.UserRole;
import com.thehartford.identityservice.model.UserStatus;
import com.thehartford.identityservice.repository.UserRepository;
import com.thehartford.identityservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Core service for identity operations: registration, login, token validation,
 * and user profile retrieval. All operations are fully reactive (non-blocking).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // ──────────────────────────────────────────────────────────
    // Registration
    // ──────────────────────────────────────────────────────────

    /**
     * Register a new user. Fails if the email is already in use.
     * userId is NOT set — MySQL AUTO_INCREMENT assigns it.
     */
    public Mono<UserResponse> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException(
                                "Email is already registered: " + request.getEmail()));
                    }

                    User user = User.builder()
                            .name(request.getName())
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role(UserRole.PROPERTY_OWNER)
                            .status(UserStatus.ACTIVE)
                            .build();

                    return userRepository.save(user);
                })
                .map(this::toUserResponse)
                .doOnSuccess(u -> log.info("User registered: {} [{}]", u.getEmail(), u.getRole()));
    }

    // ──────────────────────────────────────────────────────────
    // Login
    // ──────────────────────────────────────────────────────────

    /**
     * Authenticate a user by email and password, returning a JWT on success.
     */
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid email or password")))
                .flatMap(user -> {
                    if (user.getStatus() == UserStatus.SUSPENDED) {
                        return Mono.error(new IllegalStateException("Account is suspended"));
                    }
                    if (user.getStatus() == UserStatus.INACTIVE) {
                        return Mono.error(new IllegalStateException("Account is inactive"));
                    }
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new IllegalArgumentException("Invalid email or password"));
                    }

                    String token = jwtService.generateToken(user);
                    Instant expiresAt = jwtService.getExpiry(token);

                    AuthResponse response = AuthResponse.builder()
                            .token(token)
                            .tokenType("Bearer")
                            .userId(String.valueOf(user.getUserId()))  // Long → String for JSON response
                            .email(user.getEmail())
                            .role(user.getRole())
                            .expiresAt(expiresAt)
                            .build();

                    log.info("User logged in: {} [{}]", user.getEmail(), user.getRole());
                    return Mono.just(response);
                });
    }

    // ──────────────────────────────────────────────────────────
    // Token Validation
    // ──────────────────────────────────────────────────────────

    /**
     * Validate a JWT token and return the decoded claims.
     */
    public Mono<ValidateTokenResponse> validateToken(String token) {
        return jwtService.validateToken(token)
                .map(claims -> ValidateTokenResponse.builder()
                        .valid(true)
                        .userId(jwtService.extractUserId(claims))   // String from JWT sub
                        .email(jwtService.extractEmail(claims))
                        .role(jwtService.extractRole(claims))
                        .message("Token is valid")
                        .build())
                .onErrorResume(ex -> Mono.just(ValidateTokenResponse.builder()
                        .valid(false)
                        .message(ex.getMessage())
                        .build()));
    }

    // ──────────────────────────────────────────────────────────
    // User Queries
    // ──────────────────────────────────────────────────────────

    /**
     * Get a user profile by their numeric ID (parsed from JWT sub or path variable).
     */
    public Mono<UserResponse> getUserById(String userId) {
        long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return Mono.error(new IllegalArgumentException("Invalid user ID: " + userId));
        }
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
                .map(this::toUserResponse);
    }

    // ──────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
