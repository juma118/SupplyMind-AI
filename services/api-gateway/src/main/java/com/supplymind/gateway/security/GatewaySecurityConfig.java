package com.supplymind.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Validates every request's JWT before it reaches a route - this is what makes the
 * Gateway (README §4) the actual authentication boundary, not a decoration in front of
 * services that would still trust an unauthenticated request if hit directly.
 *
 * jwk-set-uri and the expected issuer are configured separately rather than relying on
 * OIDC discovery from a single issuer-uri: Keycloak's issuer claim reflects whatever host
 * the browser used to reach it (http://localhost:8180/realms/supplymind), but the Gateway
 * container must fetch signing keys from Keycloak's in-network address
 * (http://keycloak:8080/...) - those two addresses are never the same URL.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Value("${aiops.keycloak.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${aiops.keycloak.issuer}")
    private String expectedIssuer;

    @Value("${aiops.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(
                List.of(new JwtTimestampValidator(), new JwtIssuerValidator(expectedIssuer)));
        decoder.setJwtValidator(withIssuer);
        return decoder;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())))
                .build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
