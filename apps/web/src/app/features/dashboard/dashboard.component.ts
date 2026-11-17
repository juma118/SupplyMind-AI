import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AiOperationsApiService, AuditLogEntry } from '../../core/api/ai-operations-api.service';
import { OperationsApiService } from '../../core/api/operations-api.service';
import { ShipmentApiService } from '../../core/api/shipment-api.service';

interface Kpi {
  label: string;
  value: string;
  icon: string;
  link: string;
  warn?: boolean;
  /** Backed by actual agent/model output, not deterministic math - gets the gradient badge. */
  aiPowered?: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatListModule, MatProgressSpinnerModule, RouterLink],
  template: `
    <h2>Overview Dashboard</h2>
    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else {
      <div class="grid">
        @for (kpi of kpis(); track kpi.label) {
          <a [routerLink]="kpi.link" class="kpi-link">
            <mat-card [class.warn]="kpi.warn">
              <mat-card-content>
                <div class="icon-badge" [class.warn]="kpi.warn" [class.ai-powered]="kpi.aiPowered">
                  <mat-icon>{{ kpi.icon }}</mat-icon>
                </div>
                <div class="value">{{ kpi.value }}</div>
                <div class="label">{{ kpi.label }}</div>
              </mat-card-content>
            </mat-card>
          </a>
        }
      </div>

      <h2 class="section-heading">Recent Activity</h2>
      <mat-card class="activity-card">
        @if (activity().length === 0) {
          <mat-card-content>
            <p class="hint">No recent activity.</p>
          </mat-card-content>
        } @else {
          <mat-list>
            @for (entry of activity(); track entry.id) {
              <mat-list-item>
                <mat-icon matListItemIcon class="activity-icon">{{ activityIcon(entry) }}</mat-icon>
                <div matListItemTitle>{{ entry.action }} &middot; {{ entry.entityType }}</div>
                <div matListItemLine class="hint">
                  {{ entry.actor }} &middot; {{ entry.occurredAt | date: 'short' }}
                </div>
              </mat-list-item>
            }
          </mat-list>
        }
      </mat-card>
    }
  `,
  styles: [`
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; }
    .kpi-link { text-decoration: none; color: inherit; }
    mat-card {
      text-align: center;
      padding: 0.5rem;
      transition: box-shadow 0.15s ease, transform 0.15s ease;
    }
    .kpi-link:hover mat-card {
      box-shadow: 0 4px 14px rgba(0,0,0,0.12);
      transform: translateY(-2px);
    }
    mat-card.warn { background: var(--sm-risk-high-bg); }
    .icon-badge {
      width: 44px;
      height: 44px;
      margin: 0 auto 0.5rem;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(26, 35, 126, 0.08);
      color: #1a237e;
    }
    .icon-badge.warn { background: rgba(179, 38, 30, 0.12); color: #b3261e; }
    .icon-badge.ai-powered:not(.warn) { background: var(--sm-ai-grad); color: #fff; }
    .value { font-size: 2rem; font-weight: 600; margin: 0.25rem 0; }
    .label { color: var(--sm-text-secondary); font-size: 0.85rem; }
    .section-heading { margin-top: 2rem; }
    .activity-card { padding: 0 0.5rem; }
    .activity-icon { color: rgba(0,0,0,0.5); }
    .hint { color: var(--sm-text-secondary); font-size: 0.85rem; margin: 0; }
  `],
})
export class DashboardComponent implements OnInit {
  readonly kpis = signal<Kpi[]>([]);
  readonly activity = signal<AuditLogEntry[]>([]);
  readonly loading = signal(true);

  constructor(
    private readonly aiOpsApi: AiOperationsApiService,
    private readonly operationsApi: OperationsApiService,
    private readonly shipmentApi: ShipmentApiService,
  ) {}

  ngOnInit(): void {
    forkJoin({
      investigations: this.aiOpsApi.listInvestigations(),
      pendingApprovals: this.aiOpsApi.listPendingRecommendations(),
      stockoutRisk: this.operationsApi.getStockoutRisk(),
      atRiskShipments: this.shipmentApi.listAtRisk(),
      onTimeRate: this.shipmentApi.getOnTimeRate(),
      auditLogs: this.aiOpsApi.listAuditLogs(),
    }).subscribe({
      next: ({ investigations, pendingApprovals, stockoutRisk, atRiskShipments, onTimeRate, auditLogs }) => {
        const openInvestigations = investigations.filter((i) => i.status !== 'CLOSED').length;
        const atRiskSkus = stockoutRisk.filter((r) => r.belowReorderPoint || r.coverShorterThanLeadTime).length;

        this.kpis.set([
          {
            // null means no delivered shipment yet - shown as "-" rather than a
            // misleading 0%, which would read as "100% late" instead of "no data".
            label: 'On-Time Delivery',
            value: onTimeRate.onTimeRate != null ? `${(onTimeRate.onTimeRate * 100).toFixed(1)}%` : '-',
            icon: 'local_shipping',
            link: '/shipments',
          },
          {
            label: 'Open Investigations',
            value: String(openInvestigations),
            icon: 'smart_toy',
            link: '/ai-operations',
            aiPowered: true,
          },
          {
            label: 'Pending Approvals',
            value: String(pendingApprovals.length),
            icon: 'task_alt',
            link: '/approvals',
            warn: pendingApprovals.length > 0,
            aiPowered: true,
          },
          {
            label: 'SKUs at Stockout Risk',
            value: String(atRiskSkus),
            icon: 'inventory_2',
            link: '/forecasting',
            warn: atRiskSkus > 0,
          },
          {
            label: 'Shipments At Risk',
            value: String(atRiskShipments.length),
            icon: 'local_shipping',
            link: '/shipments',
            warn: atRiskShipments.length > 0,
          },
        ]);
        this.activity.set(
          [...auditLogs]
            .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())
            .slice(0, 8),
        );
        this.loading.set(false);
      },
      error: () => {
        // forkJoin fails all-or-nothing if any service is unreachable; leaving kpis empty
        // here at least fails visibly instead of showing stale or fabricated numbers.
        this.kpis.set([]);
        this.loading.set(false);
      },
    });
  }

  activityIcon(entry: AuditLogEntry): string {
    const type = entry.entityType?.toLowerCase() ?? '';
    if (type.includes('investigation')) return 'smart_toy';
    if (type.includes('recommendation')) return 'task_alt';
    if (type.includes('shipment')) return 'local_shipping';
    return 'history';
  }
}
