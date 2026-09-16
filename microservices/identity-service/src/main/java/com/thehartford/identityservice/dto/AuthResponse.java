package com.thehartford.identityservice.dto;

import com.thehartford.identityservice.model.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Response payload returned after successful login or registration.
 * Contains the JWT access token and user metadata.
 */
@Data
@Builder
public class AuthResponse {

    private String token;
    private String userId;
    private String email;
    private UserRole role;
    private Instant expiresAt;
}
