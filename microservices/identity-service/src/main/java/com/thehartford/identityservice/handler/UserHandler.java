package com.thehartford.identityservice.handler;

import com.thehartford.identityservice.security.JwtAuthWebFilter;
import com.thehartford.identityservice.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebFlux functional handler for user profile endpoints.
 * Handles: get user by ID, get current authenticated user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserService userService;

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/users/{userId}
    // ──────────────────────────────────────────────────────────

    /**
     * Get a user profile by their userId path variable.
     * Requires a valid JWT in the Authorization header.
     *
     * @param request the incoming ServerRequest
     * @return 200 OK with UserResponse, or 404 if not found
     */
    public Mono<ServerResponse> getUserById(ServerRequest request) {
        // Guard: JWT must be present
        Claims claims = JwtAuthWebFilter.getClaimsFromExchange(request.exchange());
        if (claims == null) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .bodyValue(Map.of("error", "Authentication required"));
        }

        String userId = request.pathVariable("userId");

        return userService.getUserById(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", ex.getMessage())));
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/v1/users/me
    // ──────────────────────────────────────────────────────────

    /**
     * Get the currently authenticated user's profile using the JWT subject (userId).
     *
     * @param request the incoming ServerRequest
     * @return 200 OK with UserResponse, or 401 if no token
     */
    public Mono<ServerResponse> getCurrentUser(ServerRequest request) {
        Claims claims = JwtAuthWebFilter.getClaimsFromExchange(request.exchange());
        if (claims == null) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("error", "Authentication required"));
        }

        String userId = claims.getSubject();

        return userService.getUserById(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        ServerResponse.status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", ex.getMessage())));
    }
}
