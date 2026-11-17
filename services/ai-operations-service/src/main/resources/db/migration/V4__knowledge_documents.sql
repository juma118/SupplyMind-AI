-- Source-of-truth copy of every ingested document, independent of whether embedding
-- succeeded. Spring AI's PgVectorStore (migration V3) only stores what it can embed; if
-- the embedding provider is unreachable at ingest time, this table is what keyword search
-- falls back to - same resilience shape as AiGatewayService's online/offline/fallback chain.
CREATE TABLE knowledge_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    source_type     VARCHAR(32) NOT NULL,
    content         TEXT NOT NULL,
    embedded        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_documents_tenant ON knowledge_documents (tenant_id);
