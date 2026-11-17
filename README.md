# SupplyMind AI
## Enterprise Agentic AI Platform for Intelligent Supply Chain Operations

> Portfolio project: a multi-tenant, event-driven supply chain platform that uses specialized AI agents to investigate disruptions, assess inventory and supplier risk, recommend recovery actions, and route consequential actions through human approval.

![SupplyMind AI architecture](./supplymind-architecture.svg)

## 1. Product vision

SupplyMind AI helps manufacturers and distributors monitor shipments, inventory, suppliers, procurement, and demand. When an operational disruption occurs, the platform correlates business data, investigates the issue, evaluates recovery options, and prepares an evidence-backed recommendation.

**Core differentiator:** agentic AI coordinates domain-specific agents and backend tools, while deterministic business logic and human approvals keep operational actions controlled and auditable.

### Primary demo scenario

1. A critical shipment carrying 5,000 units is delayed by 72 hours.
2. Warehouse A has approximately 48 hours of stock remaining.
3. A shipment event is consumed from Kafka.
4. The platform identifies affected warehouses and estimates the stockout timeline.
5. AI agents investigate alternate suppliers, available inventory, and transfer options.
6. The system creates a recovery recommendation with evidence and estimated impact.
7. A manager approves or rejects the recommendation.
8. The approved action is executed through a protected domain API and recorded in the audit log.

## 2. User roles

- **Platform Admin** — manages platform configuration and tenant provisioning.
- **Supply Chain Manager** — oversees operations and approves recovery plans.
- **Procurement Manager** — manages suppliers, requisitions, and purchase orders.
- **Warehouse Manager** — manages stock, transfers, and inventory adjustments.
- **Logistics Coordinator** — monitors shipments and carrier events.
- **Operations Analyst** — reviews forecasts, KPIs, and investigations.
- **Auditor** — reviews approvals, actions, and audit history.

Use tenant-scoped RBAC. Tenant identity must be derived from authenticated identity/context, not trusted from a browser-supplied `tenantId`.

## 3. Functional modules

1. **Organization and access** — tenants, users, roles, permissions.
2. **Supply chain master data** — products/SKUs, warehouses, suppliers, supplier catalogs.
3. **Inventory** — available, reserved, incoming, and damaged stock; movement ledger; transfers; reorder thresholds.
4. **Procurement** — requisitions, quotes, purchase orders, supplier performance.
5. **Shipment management** — carrier tracking, shipment events, ETA, disruptions.
6. **Agentic operations** — investigations, agent runs, evidence, recommendations, approvals, execution.
7. **Demand intelligence** — demand forecasting and stockout prediction.
8. **Analytics and audit** — inventory valuation, stockout risk, on-time delivery, supplier reliability, procurement cost, AI operations KPIs.

## 4. Recommended architecture

Start with four independently deployable Java services rather than creating a large microservice estate immediately.

| Component | Responsibility | Suggested technology |
|---|---|---|
| Web application | Operations console and dashboards | Angular, TypeScript, Angular Material |
| Core Service | Tenants, users, products, warehouses, suppliers | Java, Spring Boot |
| Operations Service | Inventory, replenishment, procurement | Java, Spring Boot |
| Shipment Service | Shipment lifecycle, carrier events, tracking | Java, Quarkus |
| AI Operations Service | Agent orchestration, investigations, recommendations, approvals | Java, Spring Boot, Spring AI |
| API Gateway | Routing and cross-cutting API policies | Spring Cloud Gateway |
| Identity | Authentication, roles, JWT | Keycloak |
| Event backbone | Business events and asynchronous processing | Apache Kafka |
| Transactional data | Operational persistence | PostgreSQL |
| Cache / coordination | Caching, locks, short-lived state | Redis |
| Search | Keyword and structured search | Elasticsearch |
| Vector retrieval | Policy and document retrieval | pgvector |
| Object storage | Contracts, PDFs, supporting documents | MinIO / S3 |
| Observability | Metrics, traces, dashboards | OpenTelemetry, Prometheus, Grafana |

Use Docker Compose for local development. Introduce Kubernetes when deployment complexity and operational needs justify it.

### Technology choices

- **Java:** Java 21 LTS or a currently supported LTS release.
- **Backend:** Spring Boot for core business services; Quarkus for shipment service to demonstrate both ecosystems.
- **AI:** Spring AI as the primary integration library, used specifically because its `ChatModel` abstraction decouples agent code from any one provider — this is what makes the online/offline model modes in §5.1 a configuration choice instead of two separate implementations. Consider LangChain4j only if a concrete orchestration need justifies it; avoid adding both only for technology-list breadth.
- **Frontend:** Angular standalone components, lazy routes, typed API clients, Signals. Add NgRx only if shared state complexity warrants it.
- **Forecasting:** begin with a transparent statistical baseline; add XGBoost or a dedicated Python component if it provides measurable value.
- **Optimization:** deterministic Java algorithms first; OR-Tools can be added for more advanced constraints.

## 5. Agentic AI design

Use a supervisor-style workflow with narrowly scoped specialist agents.

### 5.1 AI provider modes: online API vs. offline local

Support two interchangeable AI provider modes, selected per environment (or per tenant, for data-residency-sensitive customers) via configuration — not via separate code paths. Spring AI's `ChatModel` interface is the seam: agents call the interface, never a provider SDK directly.

| | **Online mode** | **Offline/local mode** |
|---|---|---|
| Provider | Hosted API (e.g. Anthropic, OpenAI, Bedrock) | Self-hosted inference server (e.g. Ollama, vLLM, LM Studio) on local/on-prem hardware |
| Spring AI integration | `spring-ai-anthropic` / `spring-ai-openai` / `spring-ai-bedrock` starter | `spring-ai-ollama` starter pointed at a local endpoint |
| Example models | Claude (Sonnet/Opus), GPT-4-class models | Llama 3.3 70B, Qwen2.5 32B/72B, Mistral Large, or any Ollama-served GGUF model |
| Strengths | Highest tool-calling reliability and structured-output accuracy; no local GPU required | Data never leaves the network; no per-token cost; works with no internet access |
| Trade-offs | Per-token cost; data leaves the network; requires connectivity | Weaker multi-step tool selection/structured output at smaller sizes; needs real GPU hardware (24GB+ VRAM minimum, more for 70B-class models) for usable latency |
| Typical use | Demo/portfolio deployment, cloud environments | Air-gapped environments, cost-sensitive high-volume use, data-sovereignty requirements |

Design implications:

- Store the active mode and selected model as per-tenant configuration (see §15 "Settings" — AI Models), not as a build-time flag.
- Keep a provider-agnostic model registry (provider, model name, context window, supports-structured-output, supports-tool-calling) so the UI can list only models that satisfy each agent's requirements.
- Define an explicit fallback chain (e.g. primary online model → secondary online model → local model) and apply the existing "model fallback and provider timeouts" hardening item (§15) to cover both directions — including falling back to local mode if the online API is unreachable.
- Validate that any model selected for offline mode actually supports structured JSON output and function/tool calling before allowing it to be assigned to an agent; do not assume parity with hosted models.
- Record which provider and model produced each agent run in the audit trail (already required by §5 tool-safety rules) so offline- and online-generated recommendations remain distinguishable after the fact.

### Agents

- **Operations Coordinator** — classifies incidents, plans investigation steps, delegates tasks, tracks progress, and consolidates results.
- **Shipment Intelligence Agent** — reads shipment events, carrier performance, and ETA data.
- **Inventory Intelligence Agent** — checks stock, demand, stockout dates, and transfer candidates.
- **Supplier Intelligence Agent** — compares supplier reliability, alternatives, quotes, and risk.
- **Procurement Agent** — prepares requisitions or purchase-order drafts and checks policy; it cannot independently authorize or submit them.
- **Recommendation Evaluator** — checks evidence, business constraints, policy compliance, and completeness before a recommendation is shown.

### Workflow state machine

`RECEIVED → PLANNING → INVESTIGATING → EVALUATING → RECOMMENDATION_READY → AWAITING_APPROVAL → APPROVED / REJECTED / REVISION_REQUIRED → EXECUTING → COMPLETED / FAILED → CLOSED`

Persist workflow state in the database initially. Consider Temporal later if long-running, resumable workflows become difficult to manage.

### Tool safety and control boundaries

- Expose strongly typed, narrow tools that call authenticated internal domain services.
- Enforce tenant and user authorization in backend services for every tool call.
- Do not give the model arbitrary SQL or direct database access.
- Use deterministic Java code for stock calculations, cost calculations, and policy constraints.
- Validate structured model output against JSON Schema.
- Treat retrieved documents and external content as untrusted input; defend against prompt injection.
- Apply maximum steps, deadlines, token budgets, and idempotency controls.
- Require human approval for consequential operations such as purchase-order submission or inventory transfer execution.
- Store evidence references, model/version metadata, workflow transitions, approvals, and tool-call audit records; redact secrets and unnecessary personal data.
- Do not present model confidence as a calibrated probability unless it has been calibrated.

### RAG knowledge sources

- Supplier contracts
- Procurement policies
- Shipping agreements
- Warehouse SOPs
- Product specifications

Use PostgreSQL/pgvector for vector retrieval and Elasticsearch for keyword/structured search. Copilot answers and recommendations should cite operational records or document references.

## 6. Forecasting and inventory intelligence

Forecast daily demand per SKU and warehouse. Candidate features include historical sales, weekday/month, seasonality, promotions, stock availability, supplier lead time, and supplier performance.

Start with a baseline forecast and compare more advanced models against it. Do not claim forecast accuracy until it has been measured on a held-out dataset.

**Reorder point:**

`ROP = (Average Daily Demand × Lead Time) + Safety Stock`

Potential optimization capabilities:
- Safety-stock calculation
- Economic order quantity (EOQ)
- Stock-transfer candidate ranking
- Holding-cost and shortage-cost comparison

Keep the calculations deterministic and explainable; let agents coordinate the workflow and explain results.

## 7. Data model

Suggested tables:

- `tenants`, `users`, `roles`, `user_roles`
- `products`, `warehouses`, `suppliers`, `supplier_products`
- `inventory_balances`, `inventory_movements`
- `purchase_orders`, `purchase_order_items`
- `shipments`, `shipment_items`, `shipment_events`
- `demand_forecasts`
- `ai_investigations`, `ai_agent_runs`, `ai_recommendations`, `human_approvals`
- `audit_logs`, `outbox_events`

### Data integrity principles

- Include `tenant_id` on tenant-owned tables.
- Apply tenant isolation in application authorization and consider PostgreSQL Row-Level Security as defense in depth.
- Enforce a unique inventory balance per `(tenant_id, warehouse_id, product_id)`.
- Keep inventory movements as an immutable ledger; update balances transactionally.
- Use row locks or optimistic concurrency where concurrent updates are possible.
- Use `NUMERIC` / Java `BigDecimal` for monetary values.
- Keep AI investigation data separate from operational source-of-truth data.
- AI recommendations must not directly mutate inventory or purchase orders.
- Manage schema changes with Flyway or Liquibase.

Create referenced tables before creating indexes on them; for example, create `shipment_events` before adding its indexes.

## 8. API outline

Use versioned REST endpoints under `/api/v1`, OpenAPI documentation, request validation, pagination, and a consistent error response format.

### Core Service

- `GET/POST /products`
- `GET/PUT /products/{id}`
- `GET/POST /warehouses`
- `GET/POST /suppliers`
- `GET /suppliers/{id}/performance`

### Operations Service

- `GET /inventory`
- `GET /inventory/low-stock`
- `GET /inventory/{productId}/warehouses`
- `POST /inventory/transfers`
- `POST /inventory/adjustments`
- `POST /purchase-orders`
- `GET /purchase-orders/{id}`
- `POST /purchase-orders/{id}/submit`
- `POST /purchase-orders/{id}/receive`

### Shipment Service (Quarkus)

- `POST/GET /shipments`
- `GET /shipments/{id}`
- `POST /shipments/{id}/events`
- `GET /shipments/{id}/timeline`
- `GET /shipments/at-risk`
- `POST /webhooks/carriers/{carrier}`

### AI Operations Service

- `POST/GET /investigations`
- `GET /investigations/{id}`
- `GET /investigations/{id}/timeline`
- `GET /investigations/{id}/recommendations`
- `POST /recommendations/{id}/approve`
- `POST /recommendations/{id}/reject`
- `POST /recommendations/{id}/execute`
- `POST /copilot/chat`
- `GET /dashboard/operations`

An investigation request should return `202 Accepted` when processing is asynchronous. Use Server-Sent Events (SSE) for workflow progress in the first implementation.

## 9. Kafka event design

Suggested topics:

- `shipment.events.v1`
- `inventory.events.v1`
- `procurement.events.v1`
- `supplier.events.v1`
- `ai.investigations.v1`
- `ai.recommendations.v1`
- `operations.approvals.v1`

Standard event envelope:

```json
{
  "eventId": "uuid",
  "eventType": "ShipmentDelayed",
  "eventVersion": 1,
  "tenantId": "tenant-123",
  "occurredAt": "2026-11-16T12:00:00Z",
  "aggregateId": "shipment-456",
  "payload": {}
}
```

Adopt schema versioning/registry, transactional outbox and inbox patterns, idempotent consumers, retries, and dead-letter queues (DLQs). Design consumers to tolerate duplicate delivery.

## 10. Angular application

### Main pages

- Overview Dashboard
- Products & Inventory
- Warehouses
- Shipments
- Suppliers
- Procurement
- AI Operations Center
- Forecasting & Analytics
- Reports & Audit Logs
- Settings

### Most important screen: AI Operations Center

Show active investigations, severity, agent statuses, workflow step, tool activity, evidence, recommendations, estimated impact, and pending approvals.

Suggested frontend structure:

```text
apps/web/src/app/
  core/
    auth/
    guards/
    interceptors/
    api/
    tenant-context/
  shared/
  features/
    dashboard/
    inventory/
    products/
    warehouses/
    shipments/
    suppliers/
    procurement/
    ai-operations/
    approvals/
    forecasting/
    audit/
  layout/
  app.routes.ts
```

Use typed API clients, lazy-loaded feature routes, and SSE for live investigation updates.

## 11. Monorepo structure

```text
supplymind-ai/
├── apps/
│   └── web/
├── services/
│   ├── core-service/
│   ├── operations-service/
│   ├── shipment-service/
│   └── ai-operations-service/
├── contracts/
│   ├── openapi/
│   ├── events/
│   └── json-schemas/
├── infrastructure/
│   ├── docker/
│   ├── kafka/
│   ├── postgres/
│   ├── keycloak/
│   ├── prometheus/
│   ├── grafana/
│   └── kubernetes/
├── datasets/
│   ├── products/
│   ├── suppliers/
│   ├── inventory/
│   └── shipments/
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   ├── ai/
│   ├── decisions/
│   └── demo/
├── scripts/
│   ├── seed-data.sh
│   ├── generate-demo-data.py
│   └── local-dev.sh
├── .github/
│   └── workflows/
├── docker-compose.yml
├── Makefile
└── README.md
```

Organize Java packages by feature. In the AI service, separate investigation API/application/domain/infrastructure; agents; tools; recommendations; approvals; workflow; RAG; audit; security; and configuration.

### Getting started

Two ways to run the stack, depending on whether you're actively developing or just want it running:

**Fast local development** - each service runs via its own toolchain (`mvn spring-boot:run` / `mvn quarkus:dev` / `ng serve`), so edits reload without a container rebuild. Only Postgres and Kafka run in Docker.

```bash
bash scripts/local-dev.sh    # starts infra, then every service, in the background
bash scripts/seed-data.sh    # loads demo warehouses/products/suppliers once everything is up
bash scripts/stop-local-dev.sh
```

**One-command containerized run** - everything, including the Angular app, builds and runs as containers. Slower to rebuild after a code change, but closer to how it would actually deploy.

```bash
make full-up      # docker compose --profile full up --build -d
make logs
make full-down
```

Either way, the services land on the same ports: Core Service `:8081`, Operations Service `:8082`, Shipment Service `:8083`, AI Operations Service `:8084`, web app `:4200`. There is no API Gateway yet (see §4), so the web app currently calls each service's port directly - that collapses to one gateway origin once the gateway exists.

## 12. MVP roadmap

No fixed calendar timeline — phases are sequential milestones, built as fast as pace allows.

| Phase | Focus | Deliverables |
|---|---|---|
| 1 | Foundation | Monorepo, Maven services, Angular app, local infrastructure, auth/tenant context, migrations, seed data |
| 2 | Core modules | Products, suppliers, warehouses, inventory ledger, purchase orders, Quarkus shipment service, Kafka |
| 3 | Intelligence | Carrier events, delay detection, stockout calculation, transfer candidates, supplier metrics, dashboard |
| 4 | Agentic AI | Coordinator, specialist agents, tools, structured output, persistent investigations, recommendation evaluator |
| 5 | Approval and UI | Investigation timeline, evidence, approval/rejection, controlled execution, SSE, RAG |
| 6 | Quality and demo | Testcontainers, Playwright, AI evaluation dataset, observability, audit, Docker, docs, demo scenarios |

**Build one complete vertical slice first:** shipment delay → Kafka event → stockout calculation → coordinator → specialist agents → evidence-backed recommendation → Angular view → human approval → controlled action → audit record.

## 13. Testing and quality targets

- Aim for 80%+ unit coverage of core domain logic; prioritize meaningful behavior over a vanity coverage number.
- Integration tests for all critical business workflows.
- Playwright tests for authentication, inventory, investigation, and approval.
- Tenant-isolation tests across APIs, database access, and background consumers.
- Kafka tests for retries, duplicates, and DLQ handling.
- Permission tests for every action-capable tool.
- Validate every AI recommendation against a schema and business constraints.
- Require evidence references for recommendations.
- Measure actual p95 latency, investigation duration, and token/model cost; do not invent performance metrics.

### AI evaluation metrics

- Tool-selection accuracy
- Investigation completion rate
- Evidence retrieval precision
- Recommendation correctness
- Unsupported-claim rate
- Cost per investigation
- Median and p95 investigation duration

## 14. Synthetic demo dataset

Suggested seed-data scale (targets, not claims about real production data):

- 3 organizations
- 30 users
- 2,000 SKUs
- 20 warehouses
- 150 suppliers
- 40,000 inventory records
- 10,000 purchase orders
- 15,000 shipments
- 100,000 shipment events
- 1 million historical demand records

### Demo scenarios

1. Shipment disruption
2. Supplier failure
3. Demand spike
4. Inventory imbalance
5. Procurement anomaly

Lead with the shipment-disruption scenario because it demonstrates the full event-driven and agentic workflow.

## 15. Production hardening checklist

- Distributed tracing and correlation IDs
- Circuit breakers and service timeouts
- DLQ inspection and event replay
- Transactional outbox/inbox
- Database backup and restore testing
- Kubernetes deployment and secrets management
- Model fallback and provider timeouts
- Prompt/version management and regression evaluations
- Rate limits and tenant quotas
- PII/secret redaction and retention policies
- Disaster-recovery objectives and runbooks
- AI cost and latency observability

## 16. Portfolio deliverables

- Source code with clear local setup
- Architecture diagram, sequence diagrams, and ERD
- OpenAPI specifications and Kafka event contracts
- Docker Compose environment
- CI/CD pipelines
- AI evaluation report with reproducible test cases
- Seeded synthetic dataset
- Public demo deployment, if feasible
- Five repeatable demo scenarios
- 3–5 minute product walkthrough video
- Short architecture presentation explaining trade-offs and safety controls

## 17. Positioning

**Project headline:**  
“SupplyMind AI — An Enterprise Agentic AI Platform for Intelligent Supply Chain Operations”

**Professional positioning demonstrated:**  
Senior AI & Backend Engineer — Enterprise Java, Agentic AI, Distributed Systems, and Full-Stack Development.

Emphasize the engineering substance: reliable event processing, multi-tenant security, domain modeling, measurable AI evaluation, human-in-the-loop execution, and production observability.
