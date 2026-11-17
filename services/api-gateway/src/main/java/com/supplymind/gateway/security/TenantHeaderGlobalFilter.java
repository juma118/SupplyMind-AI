package com.supplymind.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * This is the actual fix for README §2's rule ("tenant identity must be derived from
 * authenticated identity/context, not trusted from a browser-supplied tenantId"). Every
 * request that reaches a downstream service now carries an X-Tenant-Id set here from the
 * validated JWT's tenant_id claim - any client-supplied X-Tenant-Id is stripped first, so
 * a caller cannot simply send a different tenant header to impersonate another tenant.
 * Downstream services' TenantFilter classes are unchanged; they just stop being the trust
 * boundary, because nothing but the Gateway is meant to reach them directly anymore.
 */
@Component
public class TenantHeaderGlobalFilter implements WebFilter, Ordered {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(Jwt::getClaims)
                // flatMap + Mono.justOrEmpty, not .map(), because the claim can legitimately
                // be absent (e.g. a client-credentials token has no tenant_id at all) -
                // Mono.map()'s contract forbids a null return and throws NullPointerException
                // instead, which would 500 every such request rather than just forward no header.
                .flatMap(claims -> Mono.justOrEmpty((String) claims.get(TENANT_CLAIM)))
                .defaultIfEmpty("")
                .flatMap(tenantId -> {
                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
                    requestBuilder.headers(headers -> headers.remove(TENANT_HEADER));
                    if (!tenantId.isBlank()) {
                        requestBuilder.header(TENANT_HEADER, tenantId);
                    }
                    ServerWebExchange mutated = exchange.mutate().request(requestBuilder.build()).build();
                    return chain.filter(mutated);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
