package com.thehartford.identityservice.dto;

import com.thehartford.identityservice.model.UserRole;
import lombok.Builder;
import lombok.Data;

/**
 * Response payload for token validation.
 * Returned to the API Gateway or other microservices calling /auth/validate.
 */
@Data
@Builder
public class ValidateTokenResponse {

    private boolean valid;
    private String userId;
    private String email;
    private UserRole role;
    private String message;
}
