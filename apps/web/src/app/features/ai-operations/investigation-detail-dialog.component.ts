import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiOperationsApiService, Evidence, InvestigationDetail } from '../../core/api/ai-operations-api.service';

/**
 * Renders the actual multi-agent pipeline for one investigation - the agent-by-agent
 * trace (InvestigationWorkflowService's sequence: Shipment/Inventory/Supplier Intelligence
 * -> Operations Coordinator -> Policy Retrieval -> Recommendation Evaluator), each step
 * showing which mode actually produced it (AiGatewayService's online/offline/fallback
 * chain) and what evidence it cited. This is the one place in the UI where "agentic AI"
 * is actually visible rather than just a label on a nav item.
 */
@Component({
  selector: 'app-investigation-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>
      <span class="title-icon"><mat-icon>smart_toy</mat-icon></span>
      Investigation Trace
    </h2>
    <mat-dialog-content>
      @if (loading()) {
        <div class="center-spinner"><mat-spinner diameter="32" /></div>
      } @else {
        @if (detail(); as d) {
          <div class="summary-row">
            <div>
              <div class="summary-label">Incident</div>
              <div class="summary-value">{{ d.investigation.incidentType }} &middot; {{ d.investigation.sourceReference }}</div>
            </div>
            <div>
              <div class="summary-label">Severity</div>
              <div class="summary-value">{{ d.investigation.severity ?? '-' }}</div>
            </div>
            <div>
              <div class="summary-label">Status</div>
              <mat-chip class="ai-chip">{{ d.investigation.status }}</mat-chip>
            </div>
          </div>

          <h3 class="section-title">Agent Pipeline</h3>
          <div class="timeline">
            @for (run of d.agentRuns; track run.id) {
              <div class="step">
                <div class="step-rail">
                  <span class="step-dot" [class]="'mode-' + (run.mode ?? 'fallback').toLowerCase()">
                    <mat-icon>{{ statusIcon(run.status) }}</mat-icon>
                  </span>
                  <span class="step-line"></span>
                </div>
                <div class="step-body">
                  <div class="step-header">
                    <span class="step-agent">{{ run.agentName }}</span>
                    <mat-chip [class]="'mode-' + (run.mode ?? 'fallback').toLowerCase()">
                      {{ run.mode ?? 'FALLBACK' }}
                    </mat-chip>
                    @if (run.provider) {
                      <span class="step-provider">{{ run.provider }} &middot; {{ run.model }}</span>
                    }
                  </div>
                  <p class="step-output">{{ run.output }}</p>
                  @if (run.errorMessage) {
                    <p class="step-error">{{ run.errorMessage }}</p>
                  }
                </div>
              </div>
            }
          </div>

          @if (d.evidence.length > 0) {
            <h3 class="section-title">Evidence Cited</h3>
            <div class="evidence-list">
              @for (e of d.evidence; track e.id) {
                <div class="evidence-row">
                  <mat-icon class="evidence-icon">{{ evidenceIcon(e) }}</mat-icon>
                  <div>
                    <div class="evidence-source">{{ e.sourceType }} &middot; {{ e.sourceId }}</div>
                    <div class="evidence-desc">{{ e.description }}</div>
                  </div>
                </div>
              }
            </div>
          }

          @if (d.recommendations.length > 0) {
            <h3 class="section-title">Recommendation</h3>
            @for (rec of d.recommendations; track rec.id) {
              <div class="recommendation-card">
                <div class="recommendation-header">
                  <mat-chip class="ai-chip">{{ rec.actionType }}</mat-chip>
                  <span class="recommendation-status">{{ rec.status }}</span>
                </div>
                <p>{{ rec.summary }}</p>
              </div>
            }
          }
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 0.6rem; }
    .title-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 9px;
      background: var(--sm-ai-grad);
    }
    .title-icon mat-icon { color: #fff; font-size: 18px; width: 18px; height: 18px; }
    .center-spinner { display: flex; justify-content: center; padding: 2rem; }
    .summary-row {
      display: flex;
      gap: 2rem;
      padding: 0.75rem 1rem;
      background: var(--sm-ai-grad-soft);
      border-radius: var(--sm-radius);
      margin-bottom: 1rem;
    }
    .summary-label { font-size: 0.75rem; color: var(--sm-text-secondary); text-transform: uppercase; letter-spacing: 0.03em; }
    .summary-value { font-weight: 500; }
    .section-title { font-size: 0.95rem; font-weight: 600; margin: 1.25rem 0 0.5rem; }
    .timeline { display: flex; flex-direction: column; }
    .step { display: flex; gap: 0.75rem; }
    .step-rail { display: flex; flex-direction: column; align-items: center; }
    .step-dot {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .step-dot mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .step-line { flex: 1; width: 2px; background: var(--sm-border); margin: 2px 0; min-height: 12px; }
    .step:last-child .step-line { display: none; }
    .step-body { padding-bottom: 1.1rem; flex: 1; }
    .step-header { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.25rem; }
    .step-agent { font-weight: 600; }
    .step-provider { font-size: 0.8rem; color: var(--sm-text-secondary); }
    .step-output { margin: 0.2rem 0 0; color: #1e293b; }
    .step-error { margin: 0.2rem 0 0; color: #b3261e; font-size: 0.85rem; }
    .evidence-list { display: flex; flex-direction: column; gap: 0.6rem; }
    .evidence-row { display: flex; gap: 0.6rem; align-items: flex-start; }
    .evidence-icon { color: var(--sm-text-secondary); font-size: 18px; width: 18px; height: 18px; margin-top: 2px; }
    .evidence-source { font-size: 0.8rem; font-weight: 500; color: var(--sm-text-secondary); }
    .evidence-desc { font-size: 0.9rem; }
    .recommendation-card {
      border: 1px solid var(--sm-border);
      border-radius: var(--sm-radius);
      padding: 0.75rem 1rem;
    }
    .recommendation-header { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.4rem; }
    .recommendation-status { font-size: 0.8rem; color: var(--sm-text-secondary); }
  `],
})
export class InvestigationDetailDialogComponent implements OnInit {
  readonly detail = signal<InvestigationDetail | null>(null);
  readonly loading = signal(true);

  constructor(
    private readonly api: AiOperationsApiService,
    @Inject(MAT_DIALOG_DATA) public data: { investigationId: string },
  ) {}

  ngOnInit(): void {
    this.api.getInvestigationDetail(this.data.investigationId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  statusIcon(status: string): string {
    if (status === 'DONE') return 'check';
    if (status === 'FAILED') return 'priority_high';
    return 'hourglass_empty';
  }

  evidenceIcon(evidence: Evidence): string {
    const type = evidence.sourceType.toLowerCase();
    if (type.includes('shipment')) return 'local_shipping';
    if (type.includes('inventory')) return 'inventory_2';
    if (type.includes('supplier')) return 'storefront';
    if (type.includes('policy')) return 'description';
    return 'fact_check';
  }
}
