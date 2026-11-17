package com.supplymind.aiops.agent;

/**
 * Which provider mode produced an agent run - see README §5.1.
 * FALLBACK means no provider was reachable/configured and a deterministic
 * rule-based substitute was used instead of a real model call.
 */
public enum AiMode {
    ONLINE,
    OFFLINE,
    FALLBACK
}
