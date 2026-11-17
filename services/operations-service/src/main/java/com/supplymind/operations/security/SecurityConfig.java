package com.supplymind.operations.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Makes this service a resource server in its own right rather than only relying on the
 * Gateway to have already checked the token - README §5's "require human approval for
 * consequential operations" means nothing if any authenticated caller can hit
 * submit/receive/transfer regardless of role. @PreAuthorize on those controller methods
 * is the actual enforcement; this class just wires up where the role claims come from.
 *
 * jwk-set-uri/issuer split for the same reason as the Gateway's GatewaySecurityConfig:
 * this container reaches Keycloak via its in-network address, but tokens carry the
 * browser-facing issuer.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${aiops.keycloak.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${aiops.keycloak.issuer}")
    private String expectedIssuer;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(
                List.of(new JwtTimestampValidator(), new JwtIssuerValidator(expectedIssuer)));
        decoder.setJwtValidator(withIssuer);
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /** Keycloak puts realm roles under realm_access.roles, not the "scope" claim Spring
     * Security's default converter expects - without this, every @PreAuthorize check would
     * silently see zero authorities and reject every request. */
    private static class KeycloakRealmRoleConverter implements org.springframework.core.convert.converter.Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.get("roles") == null) {
                return List.of();
            }
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }
    }
}
