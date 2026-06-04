package com.rental.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Global JWT filter applied to all routes.
 * Public paths (login, register, PayPal capture, GET listings) bypass JWT validation.
 * For protected paths, the filter validates the token and forwards user info as headers
 * so downstream services can trust the caller's identity.
 */
@Component
@Slf4j
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    // Paths that do NOT require a JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/payments/paypal/capture",
            "/uploads",   // uploaded listing images are publicly readable
            "/actuator"
    );

    // GET-only public paths (listing reads are public)
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/listings"
    );

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // Allow public paths without a token
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        boolean isPublicGet = "GET".equals(method) && PUBLIC_GET_PATHS.stream().anyMatch(path::startsWith);

        if (isPublic || isPublicGet) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Forward user identity to downstream services via headers
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Id", claims.get("userId", String.class))
                            .header("X-User-Email", claims.getSubject())
                            .header("X-User-Role", claims.get("role", String.class))
                    )
                    .build();

            return chain.filter(mutated);

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1; // Run before routing filters
    }
}
