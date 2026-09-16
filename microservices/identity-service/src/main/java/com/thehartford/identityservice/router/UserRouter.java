package com.thehartford.identityservice.router;

import com.thehartford.identityservice.handler.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * WebFlux functional router for user profile endpoints.
 *
 * <p>The {@code /api/v1} prefix is intentionally omitted here — it is
 * added by the API Gateway when routing inbound requests to this service.
 *
 * <pre>
 * GET /users/me          → UserHandler::getCurrentUser
 * GET /users/{userId}    → UserHandler::getUserById
 * </pre>
 *
 * <p>Note: /me must be declared BEFORE /{userId} to avoid route conflict.
 */
@Configuration
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions.route()
                .nest(path("/users"), builder -> builder
                        // /me must come before /{userId}
                        .GET("/me",
                                accept(MediaType.APPLICATION_JSON),
                                userHandler::getCurrentUser)
                        .GET("/{userId}",
                                accept(MediaType.APPLICATION_JSON),
                                userHandler::getUserById)
                )
                .build();
    }
}
