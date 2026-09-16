package com.thehartford.identityservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Temporary WebFilter in Identity Service to log incoming trusted headers
 * (X-User-Id, X-User-Email, X-User-Role) forwarded by the API Gateway.
 *
 * <p>Logs only header metadata for verification — does not log JWT or password.
 */
@Slf4j
@Component
public class HeaderLoggingWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String userId = headers.getFirst("X-User-Id");
        String email = headers.getFirst("X-User-Email");
        String role = headers.getFirst("X-User-Role");

        log.info("Incoming Request [{}] -> X-User-Id: '{}', X-User-Email: '{}', X-User-Role: '{}'",
                path,
                userId != null ? userId : "N/A",
                email != null ? email : "N/A",
                role != null ? role : "N/A");

        return chain.filter(exchange);
    }
}
