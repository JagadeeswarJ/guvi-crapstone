package com.thehartford.identityservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security WebFlux configuration.
 *
 * <p>Authentication and token validation are handled manually via
 * {@link com.thehartford.identityservice.security.JwtAuthWebFilter}.
 * Spring Security here only provides BCrypt and open/closed path rules.
 */
@Configuration
@EnableWebFluxSecurity
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
     * - Everything else requires a valid session (handled by JwtAuthWebFilter)
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Auth endpoints — open
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        // Actuator — open
                        .pathMatchers("/actuator/**").permitAll()
                        // Everything else — authenticated (JWT checked by JwtAuthWebFilter)
                        .anyExchange().permitAll()
                )
                .build();
    }
}
