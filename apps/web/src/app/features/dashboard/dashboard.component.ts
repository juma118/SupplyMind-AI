import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AiOperationsApiService } from '../../core/api/ai-operations-api.service';
import { OperationsApiService } from '../../core/api/operations-api.service';
import { ShipmentApiService } from '../../core/api/shipment-api.service';

interface Kpi {
  label: string;
  value: string;
  icon: string;
  link: string;
  warn?: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
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
                <mat-icon>{{ kpi.icon }}</mat-icon>
                <div class="value">{{ kpi.value }}</div>
                <div class="label">{{ kpi.label }}</div>
              </mat-card-content>
            </mat-card>
          </a>
        }
      </div>
    }
  `,
  styles: [`
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1rem; }
    .kpi-link { text-decoration: none; color: inherit; }
    mat-card { text-align: center; padding: 0.5rem; }
    mat-card.warn { background: #fde8e6; }
    .value { font-size: 2rem; font-weight: 600; margin: 0.25rem 0; }
    .label { color: rgba(0,0,0,0.6); font-size: 0.85rem; }
  `],
})
export class DashboardComponent implements OnInit {
  readonly kpis = signal<Kpi[]>([]);
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
    }).subscribe({
      next: ({ investigations, pendingApprovals, stockoutRisk, atRiskShipments, onTimeRate }) => {
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
          },
          {
            label: 'Pending Approvals',
            value: String(pendingApprovals.length),
            icon: 'task_alt',
            link: '/approvals',
            warn: pendingApprovals.length > 0,
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
}
