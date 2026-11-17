# SupplyMind AI — Product & UX Specification
## Enterprise Agentic AI Platform for Intelligent Supply Chain Operations

> This document expands on [README.md](./README.md) with deeper product narrative, user journeys, and concrete UI screen specifications (wireframes, components, and data contracts per screen). It is meant to be the single reference for what to build in the Angular application and why.

---

## 1. Why this platform exists

Supply chain teams drown in disconnected signals: a carrier webhook says a container is late, a warehouse system says stock is low, a supplier portal says a PO is unconfirmed — and nobody connects the three until a stockout already happened.

SupplyMind AI's bet: **let specialized AI agents do the correlation and investigation work that a human analyst would do manually across five systems, but keep every consequential decision behind a human approval.** The product is not "AI that runs your warehouse" — it's "AI that writes the incident report and the recommended fix, with evidence, in the time it takes a human to open their first tab."

**What makes it a portfolio-grade system, not a toy demo:**
- Multi-tenant RBAC enforced server-side, not just hidden UI.
- Event-driven core (Kafka) instead of a single monolith polling a database.
- A real agent-tool boundary: the LLM never touches SQL or mutates state directly.
- A full audit trail: every tool call, every evidence reference, every approval decision.
- Deterministic math (ROP, EOQ, safety stock) kept out of the LLM's hands.

---

## 2. Primary demo scenario (narrative walkthrough)

1. **12:00:00** — Carrier webhook fires: shipment `SHP-45812` (5,000 units of SKU `WID-2200`) delayed 72 hours.
2. **12:00:01** — Shipment Service publishes `ShipmentDelayed` to `shipment.events.v1`.
3. **12:00:02** — AI Operations Service consumes the event, opens an `ai_investigations` record, status `RECEIVED`.
4. **12:00:03** — Operations Coordinator agent classifies severity (checks Warehouse A's projected stockout window: ~48h of cover left) and plans investigation steps. Status → `PLANNING` → `INVESTIGATING`.
5. **12:00:05–12:00:40** — Specialist agents run in parallel:
   - Shipment Intelligence Agent pulls carrier ETA history and delay patterns for this lane.
   - Inventory Intelligence Agent computes stockout date and candidate warehouses to transfer from.
   - Supplier Intelligence Agent finds 2 alternate suppliers with stock and acceptable lead time.
6. **12:00:41** — Recommendation Evaluator checks evidence completeness, policy constraints, and cost. Status → `EVALUATING` → `RECOMMENDATION_READY`.
7. **12:00:42** — Recommendation surfaces in the AI Operations Center: *"Transfer 1,200 units from Warehouse C (2-day transit) and place an emergency PO for 3,800 units with Supplier 'Nova Components' (4-day lead time, 98% on-time rate). Estimated stockout avoided; added cost $4,230."* Status → `AWAITING_APPROVAL`.
8. **12:03:00** — Supply Chain Manager reviews evidence panel, approves. Status → `APPROVED` → `EXECUTING`.
9. **12:03:02** — Approved actions call the protected Operations Service APIs (inventory transfer, PO draft creation) — never directly from the agent.
10. **12:03:05** — Status → `COMPLETED` → `CLOSED`. Audit log shows the full chain: event → investigation → agent runs → evidence → recommendation → approval → execution → outcome.

This is the single vertical slice the phased roadmap (§12 of the README) is built to prove end-to-end before anything else.

---

## 3. User roles and what each one actually does in the UI

| Role | Primary screens | Can approve? | Can configure? |
|---|---|---|---|
| Platform Admin | Settings, Audit | No | Tenants, roles, integrations |
| Supply Chain Manager | AI Operations Center, Dashboard | Yes | No |
| Procurement Manager | Procurement, Suppliers | PO-scoped only | Supplier catalogs |
| Warehouse Manager | Warehouses, Products & Inventory | Transfer-scoped only | Reorder thresholds |
| Logistics Coordinator | Shipments | No | Carrier mappings |
| Operations Analyst | Forecasting & Analytics, Dashboard | No | No |
| Auditor | Reports & Audit Logs | No (read-only) | No |

---

## 4. UI screen specifications

Design direction: dense, data-forward operations console (think a mix of a trading desk and an incident-response tool), not a consumer dashboard. Angular Material + CDK, dark-mode-first with a light theme toggle. Every screen that shows AI output visibly labels it as AI-generated and shows a confidence/evidence trail — never presented as unquestionable fact.

Layout shell used by every screen:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ☰  SupplyMind AI        [Tenant: Acme Mfg ▾]      🔔12   👤 J. Alvarez ▾ │
├───────────┬──────────────────────────────────────────────────────────────┤
│ Dashboard │                                                              │
│ Inventory │                     < page content >                        │
│ Warehouses│                                                              │
│ Shipments │                                                              │
│ Suppliers │                                                              │
│ Procurement│                                                             │
│ AI Ops ●3 │                                                              │
│ Approvals ●2                                                            │
│ Forecasting│                                                             │
│ Audit     │                                                              │
│ Settings  │                                                              │
└───────────┴──────────────────────────────────────────────────────────────┘
```
Sidebar badges (`●N`) show live counts (open investigations, pending approvals) pushed via SSE.

---

### 4.1 Overview Dashboard

Purpose: a 10-second health check across the whole operation.

```text
┌─ Overview Dashboard ───────────────────────────────────────────────────┐
│ ┌─ On-Time Delivery ─┐ ┌─ Stockout Risk ─┐ ┌─ Open Investigations ─┐   │
│ │   94.2%   ▲1.1%    │ │   7 SKUs at risk │ │   3 active, 1 urgent  │   │
│ └────────────────────┘ └──────────────────┘ └────────────────────────┘ │
│ ┌─ Pending Approvals ┐ ┌─ Inventory Value ┐ ┌─ AI Ops Cost (24h) ──┐   │
│ │   2 awaiting you   │ │   $4.82M          │ │   $1.42 · 38 runs    │   │
│ └────────────────────┘ └──────────────────┘ └────────────────────────┘ │
│                                                                          │
│ Shipment delay trend (30d)         Stockout risk by warehouse          │
│ ▁▂▃▅▃▂▁▂▄▆▅▃▂▁▂▃▅▇▆▄▃▂▁▂▃▄▅▆▇▇      WH-A ████████░░ 82%                │
│                                      WH-C ████░░░░░░ 41%                │
│                                      WH-F ██░░░░░░░░ 18%                │
│                                                                          │
│ Recent activity feed                                                    │
│ 12:03  ✅ Recommendation #RC-991 approved by J. Alvarez                 │
│ 12:00  ⚠️  Shipment SHP-45812 delayed 72h — investigation started       │
│ 11:52  📦 PO-20231 received at Warehouse A                              │
└──────────────────────────────────────────────────────────────────────┘
```
Components: `KpiTileComponent`, `TrendSparklineComponent`, `RiskBarListComponent`, `ActivityFeedComponent` (SSE-fed). Data: `GET /dashboard/operations`.

---

### 4.2 Products & Inventory

```text
┌─ Products & Inventory ───────────────────────────────────────────────┐
│ [Search SKU/name...]  [Warehouse: All ▾]  [Status: All ▾]  [+ New SKU]│
├─────────┬──────────────┬─────────┬──────────┬──────────┬─────────────┤
│ SKU     │ Name         │ Avail.  │ Reserved │ Incoming │ Reorder Pt. │
├─────────┼──────────────┼─────────┼──────────┼──────────┼─────────────┤
│ WID-2200│ Widget Pro   │  1,180  │   300    │  3,800   │   1,500 ⚠️  │
│ GSK-0091│ Gasket Mini  │  9,420  │   120    │      0   │   2,000     │
│ BLT-1140│ Bolt M8      │ 22,015  │   890    │  5,000   │   3,000     │
└─────────┴──────────────┴─────────┴──────────┴──────────┴─────────────┘
Row expand ▾ → tabs: [Movement Ledger] [Warehouses] [Demand Forecast] [Suppliers]
```
Row-level "⚠️" flags below reorder threshold. Expanding a row opens the **Movement Ledger** (immutable, append-only — matches the data-integrity principle in README §7) and a per-warehouse breakdown. Data: `GET /inventory`, `GET /inventory/low-stock`, `GET /inventory/{productId}/warehouses`.

---

### 4.3 Warehouses

```text
┌─ Warehouses ──────────────────────────────────────────────────────────┐
│ ┌─ WH-A — Columbus, OH ──────┐ ┌─ WH-C — Reno, NV ───────────┐         │
│ │ Capacity: 82% used          │ │ Capacity: 54% used          │        │
│ │ SKUs at risk: 4             │ │ SKUs at risk: 0             │        │
│ │ Cover: 48h (WID-2200) ⚠️    │ │ Cover: 11d                  │        │
│ │ [View details] [Transfer→] │ │ [View details] [Transfer→] │         │
│ └──────────────────────────────┘ └──────────────────────────────┘      │
├─────────────────────────────────────────────────────────────────────┤
│ Pending Transfers                                                     │
│ TR-0088  WH-C → WH-A   1,200 × WID-2200   ETA 2d   [Status: In transit]│
└─────────────────────────────────────────────────────────────────────┘
```
Data: `POST /inventory/transfers`, `POST /inventory/adjustments`.

---

### 4.4 Shipments

```text
┌─ Shipments ───────────────────────────────────────────────────────────┐
│ [Status: At Risk ▾]  [Carrier: All ▾]  [Date range]                   │
├──────────┬────────────┬─────────┬────────┬───────────┬───────────────┤
│ Shipment │ Carrier    │ Origin  │ Dest   │ ETA       │ Status        │
├──────────┼────────────┼─────────┼────────┼───────────┼───────────────┤
│ SHP-45812│ Pac-Freight│ Shenzhen│ WH-A   │ +72h delay│ 🔴 Delayed    │
│ SHP-45790│ OceanLine  │ Rotterdam│ WH-C  │ On time   │ 🟢 In transit │
└──────────┴────────────┴─────────┴────────┴───────────┴───────────────┘
Detail drawer → Timeline:
  ● Booked          Mar 2  10:02
  ● Departed port   Mar 4  08:15
  ● Delay reported  Mar 9  12:00   "Port congestion, Shenzhen"
  ○ ETA Columbus    Mar 14 (was Mar 11)
  → [Linked investigation: INV-0231] [View in AI Ops →]
```
Data: `GET /shipments/at-risk`, `GET /shipments/{id}/timeline`, webhook ingestion via `POST /webhooks/carriers/{carrier}`.

---

### 4.5 Suppliers

```text
┌─ Suppliers ───────────────────────────────────────────────────────────┐
│ [Search...]  [Category ▾]  [Min reliability: 90% ▾]                   │
├─────────────────┬──────────────┬────────────┬───────────┬────────────┤
│ Supplier        │ Category     │ On-Time %  │ Lead Time │ Risk       │
├─────────────────┼──────────────┼────────────┼───────────┼────────────┤
│ Nova Components │ Electronics  │ 98.2%      │ 4 days    │ 🟢 Low     │
│ Hanshin Metals  │ Raw Material │ 87.0%      │ 11 days   │ 🟡 Medium  │
│ PacWest Supply  │ Packaging    │ 71.4%      │ 6 days    │ 🔴 High    │
└─────────────────┴──────────────┴────────────┴───────────┴────────────┘
Detail panel: performance trend chart, active POs, catalog, contract documents (RAG-indexed).
```
Data: `GET /suppliers/{id}/performance`.

---

### 4.6 Procurement

```text
┌─ Procurement ─────────────────────────────────────────────────────────┐
│ [Requisitions] [Quotes] [Purchase Orders]  ← tabs                     │
├──────────┬─────────────┬──────────┬───────────┬────────┬─────────────┤
│ PO #     │ Supplier    │ SKU(s)   │ Qty       │ Value  │ Status      │
├──────────┼─────────────┼──────────┼───────────┼────────┼─────────────┤
│ PO-20245 │ Nova Comp.  │ WID-2200 │ 3,800     │ $31.2K │ 🤖 AI Draft │
│ PO-20231 │ Hanshin     │ BLT-1140 │ 10,000    │ $8.4K  │ ✅ Received │
└──────────┴─────────────┴──────────┴───────────┴────────┴─────────────┘
🤖 AI Draft rows show a banner: "Generated by Procurement Agent from
   investigation INV-0231 — requires manager submission." [Review] [Submit]
```
Note the explicit control boundary from README §5: AI-drafted POs are visually distinct and cannot be submitted by the agent — only `POST /purchase-orders/{id}/submit` triggered by a human.

---

### 4.7 AI Operations Center — flagship screen

This is the screen the whole platform is built around. Three-pane layout.

```text
┌─ AI Operations Center ─────────────────────────────────────────────────────────────────┐
│ ┌─ Active Investigations ──────┐ ┌─ INV-0231 · Shipment Delay · Severity: HIGH ───────┐ │
│ │ 🔴 INV-0231  Shipment Delay   │ │                                                      │ │
│ │    RECOMMENDATION_READY        │ │ Workflow:                                           │ │
│ │ 🟡 INV-0229  Supplier Risk     │ │  ✅Received →✅Planning →✅Investigating →          │ │
│ │    INVESTIGATING               │ │  ✅Evaluating →🔵Recommendation Ready →             │ │
│ │ 🟢 INV-0225  Demand Spike      │ │  ○Awaiting Approval →○Executing →○Closed            │ │
│ │    CLOSED                      │ │                                                      │ │
│ │                                 │ │ ┌─ Agents ─────────────────────────────────────┐   │ │
│ │ [+ Filter: severity, status]   │ │ │ Operations Coordinator      ✅ done  (1.2s)   │   │ │
│ │                                 │ │ │ Shipment Intelligence Agent ✅ done  (3.4s)   │   │ │
│ │                                 │ │ │ Inventory Intelligence Agent✅ done  (2.1s)   │   │ │
│ │                                 │ │ │ Supplier Intelligence Agent ✅ done  (4.0s)   │   │ │
│ │                                 │ │ │ Recommendation Evaluator    ✅ done  (0.8s)   │   │ │
│ │                                 │ │ └────────────────────────────────────────────────┘   │ │
│ └────────────────────────────────┘ │                                                      │ │
│                                     │ ┌─ Tool Activity Log ──────────────────────────┐   │ │
│                                     │ │ 12:00:05  getShipmentHistory(lane=CN-US)      │   │ │
│                                     │ │ 12:00:18  getStockoutProjection(WH-A,WID-2200)│   │ │
│                                     │ │ 12:00:27  findAlternateSuppliers(WID-2200)    │   │ │
│                                     │ │ 12:00:41  evaluateRecommendation(RC-991)      │   │ │
│                                     │ └────────────────────────────────────────────────┘   │ │
│                                     │                                                      │ │
│                                     │ ┌─ Recommendation RC-991 ──────────────────────┐    │ │
│                                     │ │ Transfer 1,200 units WID-2200 from WH-C (2d) │    │ │
│                                     │ │ + Emergency PO 3,800 units w/ Nova Components │    │ │
│                                     │ │ (4d lead, 98.2% on-time)                      │    │ │
│                                     │ │ Est. impact: stockout avoided · +$4,230 cost  │    │ │
│                                     │ │ Confidence: Not calibrated — evidence-based   │    │ │
│                                     │ │                                                │    │ │
│                                     │ │ Evidence: [Shipment ETA log] [Stock ledger]   │    │ │
│                                     │ │           [Supplier catalog] [Contract §4.2]  │    │ │
│                                     │ │                                                │    │ │
│                                     │ │        [✅ Approve]  [❌ Reject]  [✏️ Revise] │    │ │
│                                     │ └────────────────────────────────────────────────┘   │ │
└─────────────────────────────────────┴──────────────────────────────────────────────────────┘
```

Key UX rules for this screen:
- The workflow stepper always matches the backend state machine from README §5 exactly — no UI-invented states.
- Every evidence chip opens the exact source record (shipment event, ledger row, supplier document) — never a paraphrase only.
- Tool Activity Log streams live via SSE while an investigation is `INVESTIGATING`; it becomes a static transcript once closed.
- "Confidence" is deliberately never shown as a percentage unless the backend has calibrated it — label text literally says "Not calibrated" to satisfy the README's anti-overclaiming principle.
- Approve/Reject/Revise map to `POST /recommendations/{id}/approve|reject`; Revise reopens with `REVISION_REQUIRED`.

Components: `InvestigationListComponent`, `WorkflowStepperComponent`, `AgentStatusTimelineComponent`, `ToolActivityLogComponent` (SSE), `RecommendationCardComponent`, `EvidenceChipComponent`.

---

### 4.8 Approvals (queue view, role-filtered)

```text
┌─ Pending Approvals ───────────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ RC-991 · Shipment Delay Recovery · Submitted 12:00:42             │ │
│ │ Transfer 1,200 units + Emergency PO · Est. cost $4,230             │ │
│ │ Evidence: 4 linked records          [Open in AI Ops] [Approve ▾]  │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ RC-988 · Supplier Risk Mitigation · Submitted 09:12:10            │ │
│ │ Switch primary supplier for GSK-0091                                │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```
A dedicated queue so a Supply Chain Manager never has to hunt through investigations to find what needs their signature — this is the screen they live in during an incident.

---

### 4.9 Forecasting & Analytics

```text
┌─ Forecasting & Analytics ─────────────────────────────────────────────┐
│ SKU: WID-2200 ▾    Warehouse: WH-A ▾    Horizon: 30d ▾                │
│                                                                        │
│  Units                                                                │
│  2000 ┤                                        ╭── forecast (p50)     │
│  1500 ┤                 actual ╮          ╭────╯                     │
│  1000 ┤ ╭────╮────╮────╮─────╯╰────╮────╯  ┄┄┄┄ forecast band (p10-90)│
│   500 ┤─╯                           ╰┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│       └─────────────────────────────────────────────────────────────  │
│         Day -30            Day 0 (today)              Day +30        │
│                                                                        │
│ Reorder point: 1,500   Safety stock: 420   EOQ: 2,200                │
│ Model: baseline-moving-avg · backtested MAPE: 14.2% (30d holdout)     │
└────────────────────────────────────────────────────────────────────┘
```
MAPE/backtest numbers are always shown next to any forecast chart — the README is explicit that accuracy must never be claimed without measurement.

---

### 4.10 Reports & Audit Logs

```text
┌─ Audit Logs ──────────────────────────────────────────────────────────┐
│ [Entity: Recommendation ▾] [Actor: All ▾] [Date range] [Export CSV]   │
├──────────┬────────────────┬──────────────┬─────────────┬─────────────┤
│ Time     │ Actor          │ Action       │ Entity      │ Details     │
├──────────┼────────────────┼──────────────┼─────────────┼─────────────┤
│ 12:03:00 │ J. Alvarez     │ APPROVE      │ RC-991      │ [View diff] │
│ 12:00:42 │ system (agent) │ RECOMMEND    │ RC-991      │ [View run]  │
│ 12:00:02 │ system (kafka) │ INGEST       │ SHP-45812   │ [View event]│
└──────────┴────────────────┴──────────────┴─────────────┴─────────────┘
```
Every row is immutable and traceable back to a Kafka event, an agent run, or a human action — satisfying README §9's audit requirements.

---

### 4.11 Settings

```text
┌─ Settings ────────────────────────────────────────────────────────────┐
│ [Tenants] [Users & Roles] [Integrations] [Reorder Policies] [AI Models]│
│                                                                        │
│ AI Models                                                             │
│  Mode:  ◉ Online (hosted API)   ○ Offline (local inference)          │
│  ┌─ Online configuration ──────────────────────────────────────────┐ │
│  │ Provider: Anthropic ▾   Model: claude-sonnet-5 ▾                │ │
│  │ Fallback: claude-opus-5 ▾                                       │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│  ┌─ Offline configuration (disabled while Online is active) ──────┐ │
│  │ Inference server: Ollama ▾   Endpoint: http://localhost:11434  │ │
│  │ Model: qwen2.5:32b ▾   ⚠️ supports tool-calling: yes            │ │
│  │ Fallback: llama3.3:70b ▾                                        │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│  Fallback chain: Online primary → Online fallback → Offline model     │
│  Per-agent model override: [Operations Coordinator: default ▾] ...   │
│  Max steps/investigation: 12   Token budget: 40K   Timeout: 90s       │
│  [Model registry →]  [Prompt version history →]  [Evaluation dashboard →]│
└────────────────────────────────────────────────────────────────────┘
```
Visible/admin-editable because README §15 requires model fallback, prompt versioning, and cost/latency observability to be real operational controls, not hidden config. The Mode toggle and per-mode panels map directly to README §5.1 (online API vs. offline local providers) — switching mode is a tenant-level configuration change via Spring AI's `ChatModel` abstraction, never a redeploy. The model dropdowns are populated from the provider-agnostic **model registry** (provider, model name, context window, tool-calling support, structured-output support) described in §5.1, so an administrator can only select an offline model that the registry has verified supports tool-calling — the ⚠️ indicator surfaces that check directly in the UI. Every agent run's audit record (§4.10) includes which mode/provider/model produced it, so online- and offline-generated recommendations stay distinguishable after the fact.

---

## 5. Cross-cutting UI principles

1. **Never hide the machine.** Every AI-origin number, recommendation, or draft is labeled (🤖 badge) and links to its evidence and agent run — no screen presents a model output as indistinguishable from ground-truth data.
2. **Approval is a first-class object**, not a modal afterthought — it has its own queue (4.8) and its own audit trail (4.10).
3. **State machines in the UI must mirror backend state machines exactly** (the AI Ops stepper, shipment timeline, PO status) — no client-invented intermediate states.
4. **Tenant context is always visible** (top bar tenant switcher) and never editable via a hidden query param — reinforces the README §2 rule that tenant identity comes from auth context, not the browser.
5. **Live data via SSE**, not polling, for anything investigation- or shipment-status related; REST + pagination for list/table views.
6. **Dark-first operations theme** with accessible contrast for status colors (red/yellow/green always paired with an icon/text label, never color alone).

---

## 6. Angular module mapping

| Screen (§4) | Feature module | Key components |
|---|---|---|
| 4.1 Dashboard | `features/dashboard` | KpiTile, TrendSparkline, ActivityFeed |
| 4.2 Products & Inventory | `features/inventory`, `features/products` | InventoryTable, MovementLedger |
| 4.3 Warehouses | `features/warehouses` | WarehouseCard, TransferDrawer |
| 4.4 Shipments | `features/shipments` | ShipmentTable, ShipmentTimeline |
| 4.5 Suppliers | `features/suppliers` | SupplierTable, PerformanceChart |
| 4.6 Procurement | `features/procurement` | POTable, AIDraftBanner |
| 4.7 AI Operations Center | `features/ai-operations` | InvestigationList, WorkflowStepper, AgentStatusTimeline, ToolActivityLog, RecommendationCard |
| 4.8 Approvals | `features/approvals` | ApprovalQueueItem |
| 4.9 Forecasting | `features/forecasting` | ForecastChart, ROPPanel |
| 4.10 Audit | `features/audit` | AuditTable |
| 4.11 Settings | `features/settings` | TenantAdmin, ModelConfigPanel |

All feature modules are lazy-loaded; `core/tenant-context` and `core/auth` guard every route per README §11 structure.

---

## 7. Open questions worth resolving before phase 1 of the roadmap

- Exact evidence-record schema shared by all agents (so `EvidenceChipComponent` has one contract, not five).
- Whether SSE or WebSocket is used for the Tool Activity Log vs. the sidebar badge counts (SSE likely suffices for both).
- How "Revise" on a recommendation re-enters the workflow — does it re-run specific agents or the whole pipeline?
- Exact shape of the model registry record (§5.1/§4.11) and who curates it — a static seed list vs. an admin-editable table a tenant can add self-hosted models to.
- Whether per-agent model overrides (§4.11) are needed at launch, or whether a single tenant-wide mode/model setting is sufficient for the first vertical slice.
