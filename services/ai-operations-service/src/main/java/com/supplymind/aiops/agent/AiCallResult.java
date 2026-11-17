package com.supplymind.aiops.agent;

public record AiCallResult(AiMode mode, String provider, String model, String text, String error) {

    public static AiCallResult success(AiMode mode, String provider, String model, String text) {
        return new AiCallResult(mode, provider, model, text, null);
    }

    public static AiCallResult fallback(String reason) {
        return new AiCallResult(AiMode.FALLBACK, "none", "rule-based", null, reason);
    }

    public boolean isSuccess() {
        return error == null && text != null;
    }
}
