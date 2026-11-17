package com.supplymind.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This filter is the entire fix for the tenant-spoofing gap this session found and closed:
 * before it existed, any caller could set X-Tenant-Id to any value and every downstream
 * service would trust it outright. These tests exist specifically to pin that fix down -
 * a regression here silently reopens that hole with no functional symptom until someone
 * notices another tenant's data leaking.
 */
class TenantHeaderGlobalFilterTest {

    private final TenantHeaderGlobalFilter filter = new TenantHeaderGlobalFilter();

    @Test
    void realTenantClaimFromTheJwtOverridesAnySpoofedHeaderTheClientSent() {
        ServerWebExchange exchange = exchangeWithJwtAndHeader("tenant-real", "tenant-spoofed");
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = captureForwardedExchange(chain);
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Tenant-Id")).isEqualTo("tenant-real");
    }

    @Test
    void noTenantClaimMeansNoTenantHeaderIsForwardedEvenIfTheClientSentOne() {
        // If the JWT somehow has no tenant_id claim, forwarding a client-supplied value
        // would silently reopen the exact spoofing hole this filter exists to close.
        ServerWebExchange exchange = exchangeWithJwtClaims(Map.of(), "tenant-spoofed");
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = captureForwardedExchange(chain);
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Tenant-Id")).isNull();
    }

    @Test
    void unauthenticatedRequestForwardsNoTenantHeaderAtAll() {
        var request = MockServerHttpRequest.get("/core-service/api/v1/products")
                .header("X-Tenant-Id", "tenant-spoofed")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = captureForwardedExchange(chain);
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Tenant-Id")).isNull();
    }

    private ServerWebExchange exchangeWithJwtAndHeader(String realTenantId, String spoofedHeaderValue) {
        return exchangeWithJwtClaims(Map.of("tenant_id", realTenantId), spoofedHeaderValue);
    }

    private ServerWebExchange exchangeWithJwtClaims(Map<String, Object> claims, String spoofedHeaderValue) {
        var request = MockServerHttpRequest.get("/core-service/api/v1/products")
                .header("X-Tenant-Id", spoofedHeaderValue)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        JwtAuthenticationToken principal = new JwtAuthenticationToken(jwt);
        return exchange.mutate().principal(Mono.just(principal)).build();
    }

    @SuppressWarnings("unchecked")
    private ServerWebExchange captureForwardedExchange(WebFilterChain chain) {
        var captor = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        return captor.getValue();
    }
}
