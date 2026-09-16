package com.thehartford.identityservice.security;

import com.thehartford.identityservice.model.User;
import com.thehartford.identityservice.model.UserRole;
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
import java.time.Instant;
import java.util.Date;

/**
 * JWT service responsible for generating and validating JWT tokens.
 *
 * <p>Token structure (as per docs/05_token_format.md):
 * <ul>
 *   <li>sub   → user_id</li>
 *   <li>email → user email</li>
 *   <li>role  → PROPERTY_OWNER | UNDERWRITER | CLAIMS_OFFICER | ADMIN</li>
 *   <li>iss   → identity-service</li>
 *   <li>iat   → issued-at timestamp</li>
 *   <li>exp   → expiration timestamp</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a signed JWT for the given user.
     *
     * @param user the authenticated user entity
     * @return signed JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))  // sub → user_id as String
                .issuer(issuer)                              // iss → identity-service
                .issuedAt(Date.from(now))                   // iat
                .expiration(Date.from(expiry))              // exp
                .claim("email", user.getEmail())            // email
                .claim("role", user.getRole().name())       // role
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate the given JWT and return its claims reactively.
     *
     * @param token the raw JWT string
     * @return Mono of Claims if valid, Mono.error if invalid/expired
     */
    public Mono<Claims> validateToken(String token) {
        return Mono.fromCallable(() ->
                Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
        ).onErrorMap(JwtException.class, ex -> {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return new JwtException("Invalid or expired token: " + ex.getMessage());
        });
    }

    /**
     * Extract the expiry instant from a token (assumes valid token).
     */
    public Instant getExpiry(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .toInstant();
    }

    /**
     * Extract the user role from validated claims.
     */
    public UserRole extractRole(Claims claims) {
        return UserRole.valueOf(claims.get("role", String.class));
    }

    /**
     * Extract the email from validated claims.
     */
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Extract the user ID (subject) from validated claims.
     */
    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }
}
