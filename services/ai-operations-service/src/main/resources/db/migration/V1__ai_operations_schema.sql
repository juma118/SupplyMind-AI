CREATE TABLE ai_investigations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    incident_type       VARCHAR(32) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    severity            VARCHAR(16),
    source_reference    VARCHAR(255),
    summary             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ
);

CREATE INDEX idx_investigations_tenant ON ai_investigations (tenant_id);
CREATE INDEX idx_investigations_status ON ai_investigations (status);

CREATE TABLE ai_agent_runs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    investigation_id    UUID NOT NULL REFERENCES ai_investigations (id),
    agent_name          VARCHAR(120) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    mode                VARCHAR(16),
    provider            VARCHAR(64),
    model               VARCHAR(120),
    output              TEXT,
    error_message       TEXT,
    started_at          TIMESTAMPTZ NOT NULL,
    finished_at         TIMESTAMPTZ
);

CREATE INDEX idx_agent_runs_investigation ON ai_agent_runs (investigation_id);

CREATE TABLE evidence_references (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    investigation_id    UUID NOT NULL REFERENCES ai_investigations (id),
    source_type         VARCHAR(64) NOT NULL,
    source_id           VARCHAR(255),
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evidence_investigation ON evidence_references (investigation_id);

CREATE TABLE ai_recommendations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    investigation_id    UUID NOT NULL REFERENCES ai_investigations (id),
    summary             TEXT NOT NULL,
    estimated_cost      NUMERIC(14, 2),
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_investigation ON ai_recommendations (investigation_id);
CREATE INDEX idx_recommendations_tenant ON ai_recommendations (tenant_id);

CREATE TABLE human_approvals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    recommendation_id   UUID NOT NULL REFERENCES ai_recommendations (id),
    decision            VARCHAR(32) NOT NULL,
    actor_id            VARCHAR(120) NOT NULL,
    comment             TEXT,
    decided_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_approvals_recommendation ON human_approvals (recommendation_id);

CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(64) NOT NULL,
    actor               VARCHAR(120) NOT NULL,
    action              VARCHAR(64) NOT NULL,
    entity_type         VARCHAR(64) NOT NULL,
    entity_id           VARCHAR(120) NOT NULL,
    details             TEXT,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_tenant ON audit_logs (tenant_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
