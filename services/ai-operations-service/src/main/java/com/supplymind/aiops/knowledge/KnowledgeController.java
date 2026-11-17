package com.supplymind.aiops.knowledge;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    @PostMapping("/documents")
    public KnowledgeDtos.IngestDocumentResponse ingest(@Valid @RequestBody KnowledgeDtos.IngestDocumentRequest request) {
        return service.ingest(request);
    }

    @GetMapping("/search")
    public KnowledgeDtos.SearchResponse search(@RequestParam("q") String query,
                                                @RequestParam(name = "topK", defaultValue = "5") int topK) {
        return service.search(query, topK);
    }
}
