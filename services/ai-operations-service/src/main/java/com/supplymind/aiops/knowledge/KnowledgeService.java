package com.supplymind.aiops.knowledge;

import com.supplymind.aiops.common.TenantContext;
import com.supplymind.aiops.common.TenantRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * README §5 RAG: supplier contracts, procurement policies, shipping agreements, warehouse
 * SOPs, product specs. KnowledgeDocument (Postgres) is always written and is the fallback
 * retrieval path; the pgvector-backed VectorStore is the semantic path, attempted first,
 * same online-then-fallback shape as AiGatewayService (README §5.1) - the embedding
 * provider (Ollama) being unreachable degrades retrieval to keyword search rather than
 * failing the whole request.
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public KnowledgeService(KnowledgeDocumentRepository documentRepository, VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    public KnowledgeDtos.IngestDocumentResponse ingest(KnowledgeDtos.IngestDocumentRequest request) {
        String tenantId = requireTenant();

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTenantId(tenantId);
        doc.setTitle(request.title());
        doc.setSourceType(request.sourceType());
        doc.setContent(request.content());

        boolean embedded;
        String note;
        try {
            Document aiDocument = new Document(request.content(), Map.of(
                    "tenantId", tenantId,
                    "title", request.title(),
                    "sourceType", request.sourceType().name()));
            vectorStore.add(List.of(aiDocument));
            embedded = true;
            note = "Embedded and indexed for semantic search.";
        } catch (Exception e) {
            log.warn("Embedding failed for document '{}' - stored for keyword search only: {}",
                    request.title(), e.getMessage());
            embedded = false;
            note = "Embedding provider unavailable (" + e.getMessage() + ") - stored for keyword search only.";
        }
        doc.setEmbedded(embedded);
        KnowledgeDocument saved = documentRepository.save(doc);

        return new KnowledgeDtos.IngestDocumentResponse(saved.getId().toString(), embedded, note);
    }

    public KnowledgeDtos.SearchResponse search(String query, int topK) {
        String tenantId = requireTenant();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("tenantId == '" + tenantId + "'")
                    .build();
            List<Document> matches = vectorStore.similaritySearch(request);
            var results = matches.stream()
                    .map(d -> new KnowledgeDtos.SearchResult(
                            d.getId(),
                            String.valueOf(d.getMetadata().get("title")),
                            String.valueOf(d.getMetadata().get("sourceType")),
                            snippet(d.getText()),
                            d.getScore()))
                    .toList();
            return new KnowledgeDtos.SearchResponse(results, true, "Semantic search via pgvector.");
        } catch (Exception e) {
            log.warn("Semantic search unavailable, falling back to keyword search: {}", e.getMessage());
            var results = keywordSearch(tenantId, query, topK);
            return new KnowledgeDtos.SearchResponse(results, false,
                    "Embedding provider unavailable (" + e.getMessage() + ") - fell back to keyword search.");
        }
    }

    /**
     * Matches on individual significant words (any match, ranked by how many matched),
     * not the whole query phrase as one literal substring - a natural-language query like
     * "emergency procurement stockout policy" would never match a document's exact wording
     * otherwise, since real documents don't repeat a search query verbatim.
     */
    private List<KnowledgeDtos.SearchResult> keywordSearch(String tenantId, String query, int topK) {
        List<String> terms = List.of(query.toLowerCase().split("\\s+")).stream()
                .filter(t -> t.length() > 2)
                .toList();
        if (terms.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllByTenantId(tenantId).stream()
                .map(d -> Map.entry(d, countMatches(d, terms)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topK)
                .map(e -> new KnowledgeDtos.SearchResult(
                        e.getKey().getId().toString(), e.getKey().getTitle(), e.getKey().getSourceType().name(),
                        snippet(e.getKey().getContent()), null))
                .toList();
    }

    private int countMatches(KnowledgeDocument document, List<String> terms) {
        String haystack = (document.getTitle() + " " + document.getContent()).toLowerCase();
        return (int) terms.stream().filter(haystack::contains).count();
    }

    private String snippet(String content) {
        return content.length() > 240 ? content.substring(0, 240) + "..." : content;
    }

    private String requireTenant() {
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantRequiredException();
        }
        return tenantId;
    }
}
