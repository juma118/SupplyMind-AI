package com.supplymind.aiops.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class KnowledgeDtos {

    private KnowledgeDtos() {
    }

    public record IngestDocumentRequest(
            @NotBlank String title,
            @NotNull KnowledgeSourceType sourceType,
            @NotBlank String content) {
    }

    public record IngestDocumentResponse(String documentId, boolean embedded, String note) {
    }

    public record SearchResult(String documentId, String title, String sourceType, String snippet, Double score) {
    }

    public record SearchResponse(java.util.List<SearchResult> results, boolean semanticSearchAvailable, String note) {
    }
}
