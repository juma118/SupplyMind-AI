package com.supplymind.aiops.execution;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Fetches and caches an OAuth2 client-credentials token for this service's own calls to
 * Operations Service (see OperationsServiceClient). Those calls - including the read-only
 * transfer-candidate lookup during Kafka-triggered investigation processing, which has no
 * incoming user request to borrow a token from - now need their own identity, since
 * Operations Service requires authentication on every request. The "system-service" realm
 * role this client's service account carries is what the @PreAuthorize checks on
 * Operations Service's submit/receive/transfer endpoints recognize as already-approved
 * (by a human, via this service's own supply-chain-manager-gated approve endpoint).
 */
@Component
public class KeycloakServiceTokenProvider {

    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KeycloakServiceTokenProvider(@Value("${aiops.keycloak.token-uri}") String tokenUri,
                                         @Value("${aiops.keycloak.client-id}") String clientId,
                                         @Value("${aiops.keycloak.client-secret}") String clientSecret) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(tokenUri)
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        cachedToken = (String) response.get("access_token");
        int expiresIn = ((Number) response.get("expires_in")).intValue();
        // Refresh a bit early so a call never races a just-expired token.
        expiresAt = Instant.now().plusSeconds(Math.max(expiresIn - 30, 5));
        return cachedToken;
    }
}
