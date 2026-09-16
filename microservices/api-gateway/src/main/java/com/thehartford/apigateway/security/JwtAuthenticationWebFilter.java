package com.thehartford.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reactive WebFilter that performs JWT authentication at the API Gateway level.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Public endpoints (e.g., POST /api/v1/auth/register, POST /api/v1/auth/login, POST /api/v1/auth/validate, /actuator/**) bypass JWT validation.</li>
 *   <li>Protected endpoints require a valid {@code Authorization: Bearer <token>} header.</li>
 *   <li>If valid, the request (including the Authorization header) is forwarded to downstream microservices.</li>
 *   <li>If missing, invalid, or expired, returns 401 Unauthorized.</li>
 *   <li>Does NOT perform role-based authorization — RBAC is delegated to downstream services.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtService jwtService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Public endpoint path patterns that do not require JWT authentication
    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/validate",
            "/actuator/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if path is public
        if (isPublicEndpoint(request.getMethod(), path)) {
            log.debug("Public endpoint accessed: {} {}", request.getMethod(), path);
            return chain.filter(exchange);
        }

        // Extract Authorization header for protected endpoints
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        return jwtService.validateToken(token)
                .flatMap(claims -> {
                    log.debug("JWT authenticated successfully for user '{}' with role '{}'", claims.getSubject(), claims.get("role"));
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    log.warn("Unauthorized request to {}: {}", path, ex.getMessage());
                    return onError(exchange, ex.getMessage(), HttpStatus.UNAUTHORIZED);
                });
    }

    private boolean isPublicEndpoint(HttpMethod method, String path) {
        return PUBLIC_PATH_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", status.getReasonPhrase(), errMessage);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}
