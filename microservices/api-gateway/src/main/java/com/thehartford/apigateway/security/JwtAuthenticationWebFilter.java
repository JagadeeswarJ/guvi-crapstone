package com.thehartford.apigateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
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
 *   <li>Before routing any request, client-supplied {@code X-User-Id}, {@code X-User-Email}, and {@code X-User-Role} headers are stripped to prevent header spoofing.</li>
 *   <li>For protected endpoints, after successful JWT validation:
 *     <ul>
 *       <li>{@code sub} → {@code X-User-Id}</li>
 *       <li>{@code email} → {@code X-User-Email}</li>
 *       <li>{@code role} → {@code X-User-Role}</li>
 *     </ul>
 *   </li>
 *   <li>The decorated request with trusted headers (and original Authorization header) is forwarded downstream.</li>
 *   <li>Returns 401 Unauthorized for missing, invalid, or expired tokens on protected paths.</li>
 *   <li>Does NOT perform role-based authorization — RBAC is delegated to downstream services.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtService jwtService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLE = "X-User-Role";

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
            // Remove client-supplied headers to prevent spoofing
            ServerWebExchange sanitizedExchange = sanitizeExchange(exchange, null);
            return chain.filter(sanitizedExchange);
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
                    // Strip spoofed headers and inject trusted X-User-* headers from JWT claims
                    ServerWebExchange mutatedExchange = sanitizeExchange(exchange, claims);
                    return chain.filter(mutatedExchange);
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

    private ServerWebExchange sanitizeExchange(ServerWebExchange exchange, Claims claims) {
        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            private final HttpHeaders sanitizedHeaders;

            {
                this.sanitizedHeaders = new HttpHeaders();
                this.sanitizedHeaders.putAll(exchange.getRequest().getHeaders());

                // Remove client-supplied headers to prevent spoofing
                this.sanitizedHeaders.remove(HEADER_USER_ID);
                this.sanitizedHeaders.remove(HEADER_USER_EMAIL);
                this.sanitizedHeaders.remove(HEADER_USER_ROLE);

                // Inject trusted headers from claims if present
                if (claims != null) {
                    if (claims.getSubject() != null) {
                        this.sanitizedHeaders.set(HEADER_USER_ID, claims.getSubject());
                    }
                    String email = claims.get("email", String.class);
                    if (email != null) {
                        this.sanitizedHeaders.set(HEADER_USER_EMAIL, email);
                    }
                    String role = claims.get("role", String.class);
                    if (role != null) {
                        this.sanitizedHeaders.set(HEADER_USER_ROLE, role);
                    }
                }
            }

            @Override
            public HttpHeaders getHeaders() {
                return this.sanitizedHeaders;
            }
        };

        return exchange.mutate().request(decoratedRequest).build();
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
