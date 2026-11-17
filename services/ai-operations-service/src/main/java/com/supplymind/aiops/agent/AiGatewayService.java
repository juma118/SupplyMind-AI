package com.supplymind.aiops.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Single seam between agent code and any LLM provider - see README §5.1 (online API vs.
 * offline local provider modes). Mode is a configuration property, not a code branch per
 * provider, so switching is a deploy-time or (eventually) per-tenant setting, never a rewrite.
 *
 * Call chain: configured primary mode -> the other mode as fallback -> deterministic
 * FALLBACK result if neither provider is reachable, so callers always get a usable result
 * and always know which path actually produced it (recorded on AiAgentRun).
 */
@Service
public class AiGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);

    private final AnthropicChatModel anthropicChatModel;
    private final OllamaChatModel ollamaChatModel;

    @Value("${aiops.ai.mode:online}")
    private String configuredMode;

    @Value("${aiops.ai.online.model:claude-sonnet-5}")
    private String onlineModel;

    @Value("${aiops.ai.offline.model:qwen2.5:32b}")
    private String offlineModel;

    public AiGatewayService(AnthropicChatModel anthropicChatModel, OllamaChatModel ollamaChatModel) {
        this.anthropicChatModel = anthropicChatModel;
        this.ollamaChatModel = ollamaChatModel;
    }

    public AiCallResult call(String systemPrompt, String userPrompt) {
        boolean offlineFirst = "offline".equalsIgnoreCase(configuredMode);

        AiCallResult primary = offlineFirst ? tryOffline(systemPrompt, userPrompt) : tryOnline(systemPrompt, userPrompt);
        if (primary.isSuccess()) {
            return primary;
        }
        log.warn("Primary AI mode [{}] failed: {} - falling back", offlineFirst ? "offline" : "online", primary.error());

        AiCallResult secondary = offlineFirst ? tryOnline(systemPrompt, userPrompt) : tryOffline(systemPrompt, userPrompt);
        if (secondary.isSuccess()) {
            return secondary;
        }
        log.warn("Secondary AI mode also failed: {} - using deterministic fallback", secondary.error());
        return AiCallResult.fallback(primary.error() + " / " + secondary.error());
    }

    private AiCallResult tryOnline(String systemPrompt, String userPrompt) {
        try {
            String content = ChatClient.create(anthropicChatModel)
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            return AiCallResult.success(AiMode.ONLINE, "anthropic", onlineModel, content);
        } catch (Exception e) {
            return new AiCallResult(AiMode.ONLINE, "anthropic", onlineModel, null, e.getMessage());
        }
    }

    private AiCallResult tryOffline(String systemPrompt, String userPrompt) {
        try {
            String content = ChatClient.create(ollamaChatModel)
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            return AiCallResult.success(AiMode.OFFLINE, "ollama", offlineModel, content);
        } catch (Exception e) {
            return new AiCallResult(AiMode.OFFLINE, "ollama", offlineModel, null, e.getMessage());
        }
    }
}
