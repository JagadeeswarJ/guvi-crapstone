package com.thehartford.identityservice.config;

import com.thehartford.identityservice.security.TrustedHeadersSecurityFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security WebFlux configuration.
 *
 * <p>Authentication and token validation are handled externally by the API Gateway.
 * The Gateway forwards trusted identity headers (X-User-Id, X-User-Email, X-User-Role)
 * which are converted to Spring Security context by {@link TrustedHeadersSecurityFilter}.
 *
 * <p>{@code @EnableReactiveMethodSecurity} enables {@code @PreAuthorize} / {@code @PostAuthorize}
 * annotations on handler methods.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * BCrypt password encoder bean — used by UserService to hash passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * WebFlux security filter chain.
     * - Auth endpoints are public (open)
     * - Actuator health/info is public
     * - TrustedHeadersSecurityFilter runs at AUTHENTICATION order to populate security context
     * - Method-level authorization (@PreAuthorize) is enforced by Spring Security AOP
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         TrustedHeadersSecurityFilter trustedHeadersSecurityFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterAt(trustedHeadersSecurityFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .build();
    }
}
