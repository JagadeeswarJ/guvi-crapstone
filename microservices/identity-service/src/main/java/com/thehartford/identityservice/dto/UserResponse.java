package com.thehartford.identityservice.dto;

import com.thehartford.identityservice.model.UserRole;
import com.thehartford.identityservice.model.UserStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Public-facing user profile response (no password).
 */
@Data
@Builder
public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private UserRole role;
    private UserStatus status;
}
