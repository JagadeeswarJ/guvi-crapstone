package com.thehartford.identityservice.router;

import com.thehartford.identityservice.handler.AuthHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * WebFlux functional router for authentication endpoints.
 *
 * <p>The {@code /api/v1} prefix is intentionally omitted here — it is
 * added by the API Gateway when routing inbound requests to this service.
 *
 * <pre>
 * POST /auth/register  → AuthHandler::register
 * POST /auth/login     → AuthHandler::login
 * POST /auth/validate  → AuthHandler::validateToken
 * </pre>
 */
@Configuration
public class AuthRouter {

    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthHandler authHandler) {
        return RouterFunctions.route()
                .nest(path("/auth"), builder -> builder
                        .POST("/register",
                                accept(MediaType.APPLICATION_JSON),
                                authHandler::register)
                        .POST("/login",
                                accept(MediaType.APPLICATION_JSON),
                                authHandler::login)
                        .POST("/validate",
                                accept(MediaType.APPLICATION_JSON),
                                authHandler::validateToken)
                )
                .build();
    }
}
