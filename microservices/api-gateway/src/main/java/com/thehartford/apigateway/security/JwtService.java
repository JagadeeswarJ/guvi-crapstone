package com.thehartford.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Service responsible for validating JWT tokens at the API Gateway level.
 *
 * <p>Validates:
 * <ul>
 *   <li>JWT signature</li>
 *   <li>Expiration (exp)</li>
 *   <li>Issuer (iss -> identity-service)</li>
 *   <li>Required claims: sub (user_id), email, role</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer:identity-service}")
    private String expectedIssuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates the provided JWT token reactively.
     *
     * @param token raw JWT token string
     * @return Mono of Claims if valid, or Mono.error with JwtException if invalid
     */
    public Mono<Claims> validateToken(String token) {
        return Mono.fromCallable(() -> {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Validate Issuer (iss)
            if (!expectedIssuer.equals(claims.getIssuer())) {
                log.warn("JWT issuer mismatch: expected '{}', got '{}'", expectedIssuer, claims.getIssuer());
                throw new JwtException("Invalid token issuer");
            }

            // Validate Subject (sub -> user_id)
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                log.warn("JWT missing subject (user_id)");
                throw new JwtException("Token missing subject claim");
            }

            // Validate Email claim
            String email = claims.get("email", String.class);
            if (email == null || email.isBlank()) {
                log.warn("JWT missing email claim");
                throw new JwtException("Token missing email claim");
            }

            // Validate Role claim
            String role = claims.get("role", String.class);
            if (role == null || role.isBlank()) {
                log.warn("JWT missing role claim");
                throw new JwtException("Token missing role claim");
            }

            return claims;
        }).onErrorMap(ex -> {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return new JwtException("Invalid JWT token: " + ex.getMessage());
        });
    }
}
