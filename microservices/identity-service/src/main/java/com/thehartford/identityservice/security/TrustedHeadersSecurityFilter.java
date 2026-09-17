package com.thehartford.identityservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebFilter that converts API Gateway trusted identity headers into a Spring Security
 * {@link org.springframework.security.core.Authentication} and populates the
 * {@link ReactiveSecurityContextHolder}.
 *
 * <p>The API Gateway validates the JWT and forwards:
 * <ul>
 *   <li>{@code X-User-Id}    → validated user ID (JWT sub)</li>
 *   <li>{@code X-User-Email} → validated email</li>
 *   <li>{@code X-User-Role}  → validated role (e.g. ADMIN, PROPERTY_OWNER)</li>
 * </ul>
 *
 * <p>This filter converts {@code X-User-Role} into a Spring Security granted authority
 * using the {@code ROLE_} prefix convention, enabling method-level security annotations
 * such as {@code @PreAuthorize("hasRole('ADMIN')")} to work correctly.
 *
 * <p>This filter does NOT perform JWT validation. It trusts the headers only because
 * the API Gateway has already stripped any client-supplied values and injected its own.
 */
@Slf4j
@Component
public class TrustedHeadersSecurityFilter implements WebFilter {

    public static final String HEADER_USER_ID    = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLE  = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String userId = headers.getFirst(HEADER_USER_ID);
        String role   = headers.getFirst(HEADER_USER_ROLE);

        if (userId == null || role == null) {
            // No identity headers — proceed unauthenticated (auth guards or @PreAuthorize will reject if needed)
            return chain.filter(exchange);
        }

        // Convert role to Spring Security granted authority: ADMIN → ROLE_ADMIN
        String authority = "ROLE_" + role.toUpperCase();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );

        log.debug("TrustedHeaders: principal='{}' authority='{}'", userId, authority);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}
