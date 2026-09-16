package com.thehartford.identityservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload for public user registration.
 *
 * <p>Role and status are NOT accepted from the client.
 * All self-registered users are assigned:
 * <ul>
 *   <li>role   = PROPERTY_OWNER</li>
 *   <li>status = ACTIVE</li>
 * </ul>
 * Admin-controlled creation of other roles is handled separately.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
