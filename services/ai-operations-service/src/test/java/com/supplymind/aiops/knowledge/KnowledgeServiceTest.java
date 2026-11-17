package com.supplymind.aiops.knowledge;

import com.supplymind.aiops.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * This pins down a real bug found by manual testing: the original keyword fallback
 * matched the whole query phrase as one literal substring, which never matches a natural-
 * language query against real document wording (a document doesn't repeat the search
 * query verbatim). These tests cover the fixed per-term, ranked-by-match-count behavior -
 * the actual retrieval path whenever the embedding provider is unreachable (README §5.1's
 * degradation pattern, applied to RAG instead of chat).
 */
class KnowledgeServiceTest {

    private static final String TENANT = "tenant-test";

    private KnowledgeDocumentRepository documentRepository;
    private VectorStore vectorStore;
    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        documentRepository = mock(KnowledgeDocumentRepository.class);
        vectorStore = mock(VectorStore.class);
        service = new KnowledgeService(documentRepository, vectorStore);
        // Every test here exercises the fallback path - embedding provider unreachable.
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("connection refused"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void multiWordQueryMatchesADocumentThatDoesNotContainTheExactPhrase() {
        KnowledgeDocument policy = documentWith("Emergency Procurement Policy",
                "Operations may approve an emergency purchase order when a stockout risk is identified.");
        when(documentRepository.findAllByTenantId(TENANT)).thenReturn(List.of(policy));

        var response = service.search("emergency procurement stockout policy", 5);

        assertThat(response.semanticSearchAvailable()).isFalse();
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).title()).isEqualTo("Emergency Procurement Policy");
    }

    @Test
    void documentsAreRankedByHowManyQueryTermsTheyMatch() {
        KnowledgeDocument strongMatch = documentWith("Emergency Procurement Policy",
                "Emergency purchase order policy for stockout risk procurement.");
        KnowledgeDocument weakMatch = documentWith("Warehouse SOP",
                "General warehouse operating procedure, unrelated to procurement.");
        when(documentRepository.findAllByTenantId(TENANT)).thenReturn(List.of(weakMatch, strongMatch));

        var response = service.search("emergency procurement stockout policy", 5);

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).title()).isEqualTo("Emergency Procurement Policy");
    }

    @Test
    void documentsWithNoMatchingTermsAreExcludedEntirely() {
        KnowledgeDocument unrelated = documentWith("Product Spec", "Widget dimensions and material tolerances.");
        when(documentRepository.findAllByTenantId(TENANT)).thenReturn(List.of(unrelated));

        var response = service.search("emergency procurement stockout policy", 5);

        assertThat(response.results()).isEmpty();
    }

    @Test
    void resultsAreLimitedToTopK() {
        List<KnowledgeDocument> fiveDocs = List.of(
                documentWith("Policy A", "stockout emergency"), documentWith("Policy B", "stockout emergency"),
                documentWith("Policy C", "stockout emergency"), documentWith("Policy D", "stockout emergency"),
                documentWith("Policy E", "stockout emergency"));
        when(documentRepository.findAllByTenantId(TENANT)).thenReturn(fiveDocs);

        var response = service.search("stockout emergency", 2);

        assertThat(response.results()).hasSize(2);
    }

    private KnowledgeDocument documentWith(String title, String content) {
        KnowledgeDocument doc = new KnowledgeDocument();
        org.springframework.test.util.ReflectionTestUtils.setField(doc, "id", java.util.UUID.randomUUID());
        doc.setTenantId(TENANT);
        doc.setTitle(title);
        doc.setSourceType(KnowledgeSourceType.PROCUREMENT_POLICY);
        doc.setContent(content);
        return doc;
    }
}
