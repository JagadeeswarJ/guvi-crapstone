package com.thehartford.identityservice.handler;

import com.thehartford.identityservice.dto.LoginRequest;
import com.thehartford.identityservice.dto.RegisterRequest;
import com.thehartford.identityservice.dto.ValidateTokenRequest;
import com.thehartford.identityservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux functional handler for authentication endpoints.
 * Handles: register, login, validate-token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandler {

    private final UserService userService;
    private final Validator validator;

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/auth/register
    // ──────────────────────────────────────────────────────────

    /**
     * Register a new user account.
     *
     * @param request the incoming ServerRequest
     * @return 201 CREATED with UserResponse, or 400/409 on error
     */
    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
                .flatMap(body -> {
                    var errors = new BeanPropertyBindingResult(body, "registerRequest");
                    validator.validate(body, errors);
                    if (errors.hasErrors()) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(buildValidationError(errors));
                    }
                    return userService.register(body)
                            .flatMap(user -> ServerResponse.status(HttpStatus.CREATED)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(user));
                })
                .onErrorResume(IllegalArgumentException.class, ex ->
                        ServerResponse.status(HttpStatus.CONFLICT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", ex.getMessage())));
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/auth/login
    // ──────────────────────────────────────────────────────────

    /**
     * Authenticate a user and return a JWT token.
     *
     * @param request the incoming ServerRequest
     * @return 200 OK with AuthResponse, or 401 on bad credentials
     */
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .flatMap(body -> {
                    var errors = new BeanPropertyBindingResult(body, "loginRequest");
                    validator.validate(body, errors);
                    if (errors.hasErrors()) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(buildValidationError(errors));
                    }
                    return userService.login(body)
                            .flatMap(auth -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(auth));
                })
                .onErrorResume(IllegalArgumentException.class, ex ->
                        ServerResponse.status(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", ex.getMessage())))
                .onErrorResume(IllegalStateException.class, ex ->
                        ServerResponse.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", ex.getMessage())));
    }

    // ──────────────────────────────────────────────────────────
    // POST /api/v1/auth/validate
    // ──────────────────────────────────────────────────────────

    /**
     * Validate a JWT token. Called by the API Gateway or other services.
     *
     * @param request the incoming ServerRequest
     * @return 200 OK with ValidateTokenResponse (valid=true/false)
     */
    public Mono<ServerResponse> validateToken(ServerRequest request) {
        return request.bodyToMono(ValidateTokenRequest.class)
                .flatMap(body -> userService.validateToken(body.getToken()))
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result));
    }

    // ──────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────

    private Map<String, Object> buildValidationError(
            BeanPropertyBindingResult errors) {
        var fieldErrors = errors.getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();
        return Map.of(
                "error", "Validation failed",
                "details", fieldErrors
        );
    }
}
