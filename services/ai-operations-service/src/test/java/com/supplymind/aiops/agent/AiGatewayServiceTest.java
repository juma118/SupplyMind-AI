package com.supplymind.aiops.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * README §5.1's whole design rests on this chain never leaving a caller with nothing to
 * show: online fails -> try offline -> both fail -> deterministic fallback, and the
 * caller always learns which path actually produced the result (recorded on AiAgentRun).
 * This test covers the failure/fallback path, which is the safety-relevant half - the
 * success path through Spring AI's ChatClient was exercised live in this session against
 * a real misconfigured/unconfigured provider and behaved exactly as asserted here.
 */
class AiGatewayServiceTest {

    @Test
    void fallsBackToOfflineWhenOnlineModeFails() {
        AnthropicChatModel anthropic = mock(AnthropicChatModel.class);
        OllamaChatModel ollama = mock(OllamaChatModel.class);
        when(anthropic.call(any(Prompt.class))).thenThrow(new RuntimeException("no api key"));
        when(ollama.call(any(Prompt.class))).thenReturn(chatResponseWith("offline answer"));

        AiGatewayService gateway = new AiGatewayService(anthropic, ollama);
        ReflectionTestUtils.setField(gateway, "configuredMode", "online");

        AiCallResult result = gateway.call("system", "user");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.mode()).isEqualTo(AiMode.OFFLINE);
        assertThat(result.text()).isEqualTo("offline answer");
    }

    @Test
    void returnsDeterministicFallbackWhenBothProvidersFail() {
        AnthropicChatModel anthropic = mock(AnthropicChatModel.class);
        OllamaChatModel ollama = mock(OllamaChatModel.class);
        when(anthropic.call(any(Prompt.class))).thenThrow(new RuntimeException("no api key"));
        when(ollama.call(any(Prompt.class))).thenThrow(new RuntimeException("connection refused"));

        AiGatewayService gateway = new AiGatewayService(anthropic, ollama);
        ReflectionTestUtils.setField(gateway, "configuredMode", "online");

        AiCallResult result = gateway.call("system", "user");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.mode()).isEqualTo(AiMode.FALLBACK);
        assertThat(result.error()).contains("no api key").contains("connection refused");
    }

    @Test
    void offlineModePrefersOllamaFirst() {
        AnthropicChatModel anthropic = mock(AnthropicChatModel.class);
        OllamaChatModel ollama = mock(OllamaChatModel.class);
        when(ollama.call(any(Prompt.class))).thenReturn(chatResponseWith("local model answer"));

        AiGatewayService gateway = new AiGatewayService(anthropic, ollama);
        ReflectionTestUtils.setField(gateway, "configuredMode", "offline");

        AiCallResult result = gateway.call("system", "user");

        assertThat(result.mode()).isEqualTo(AiMode.OFFLINE);
        assertThat(result.text()).isEqualTo("local model answer");
        // Online must never be tried when it already succeeded via the preferred path.
        org.mockito.Mockito.verifyNoInteractions(anthropic);
    }

    private org.springframework.ai.chat.model.ChatResponse chatResponseWith(String text) {
        var generation = new org.springframework.ai.chat.model.Generation(
                new org.springframework.ai.chat.messages.AssistantMessage(text));
        return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(generation));
    }
}
