package com.thehartford.identityservice.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter that intercepts every request, extracts the Bearer JWT token
 * from the Authorization header, validates it, and stores the claims
 * in the {@link ServerWebExchange} attributes for downstream use.
 *
 * <p>Requests without a valid JWT on protected paths are rejected with 401.
 * Public paths (auth endpoints) bypass this filter via Spring Security config.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthWebFilter implements WebFilter {

    public static final String CLAIMS_ATTRIBUTE = "JWT_CLAIMS";
    public static final String USER_ID_ATTRIBUTE = "JWT_USER_ID";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // No Bearer token — pass through (Spring Security will handle authorization)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        return jwtService.validateToken(token)
                .flatMap(claims -> {
                    // Store validated claims in exchange attributes
                    exchange.getAttributes().put(CLAIMS_ATTRIBUTE, claims);
                    exchange.getAttributes().put(USER_ID_ATTRIBUTE, claims.getSubject());
                    log.debug("JWT validated for user: {}", claims.getSubject());
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    log.warn("JWT filter error: {}", ex.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * Utility to retrieve validated JWT claims from exchange attributes.
     */
    public static Claims getClaimsFromExchange(ServerWebExchange exchange) {
        return exchange.getAttribute(CLAIMS_ATTRIBUTE);
    }

    /**
     * Utility to retrieve the validated user ID from exchange attributes.
     */
    public static String getUserIdFromExchange(ServerWebExchange exchange) {
        return exchange.getAttribute(USER_ID_ATTRIBUTE);
    }
}
