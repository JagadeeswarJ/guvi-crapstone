package com.thehartford.identityservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for token validation (called by API Gateway or other services).
 */
@Data
public class ValidateTokenRequest {

    @NotBlank(message = "Token is required")
    private String token;
}
