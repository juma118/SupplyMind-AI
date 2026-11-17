import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { CoreApiService, Warehouse } from '../../core/api/core-api.service';
import { InventoryBalance, OperationsApiService } from '../../core/api/operations-api.service';

interface WarehouseCard extends Warehouse {
  totalAvailable: number;
  skuCount: number;
  atRiskSkus: number;
}

@Component({
  selector: 'app-warehouses',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatProgressSpinnerModule],
  template: `
    <h2>Warehouses</h2>
    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <div class="grid">
        @for (wh of warehouses(); track wh.id) {
          <mat-card>
            <mat-card-title>{{ wh.code }} - {{ wh.name }}</mat-card-title>
            <mat-card-subtitle>{{ wh.city }}, {{ wh.country }}</mat-card-subtitle>
            <mat-card-content>
              <p>Capacity: {{ wh.capacityUnits ?? '-' }} units</p>
              <p>Available stock across SKUs: {{ wh.totalAvailable }}</p>
              <p>Distinct SKUs stocked: {{ wh.skuCount }}</p>
              @if (wh.atRiskSkus > 0) {
                <p class="warn">{{ wh.atRiskSkus }} SKU(s) below reorder point</p>
              }
            </mat-card-content>
          </mat-card>
        }
      </div>
      @if (warehouses().length === 0) {
        <p class="hint">No warehouses yet - run scripts/seed-data.sh or create one via the Core Service API.</p>
      }
    }
  `,
  styles: [`
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 1rem; }
    .error { color: #b3261e; }
    .warn { color: #b3261e; font-weight: 600; }
    .hint { color: rgba(0,0,0,0.6); }
  `],
})
export class WarehousesComponent implements OnInit {
  readonly warehouses = signal<WarehouseCard[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly coreApi: CoreApiService,
    private readonly operationsApi: OperationsApiService,
  ) {}

  ngOnInit(): void {
    forkJoin({
      warehouses: this.coreApi.listWarehouses(),
      balances: this.operationsApi.listInventory(),
    }).subscribe({
      next: ({ warehouses, balances }) => {
        const byWarehouse = new Map<string, InventoryBalance[]>();
        for (const balance of balances) {
          const list = byWarehouse.get(balance.warehouseCode) ?? [];
          list.push(balance);
          byWarehouse.set(balance.warehouseCode, list);
        }
        this.warehouses.set(
          warehouses.map((wh) => {
            const whBalances = byWarehouse.get(wh.code) ?? [];
            return {
              ...wh,
              totalAvailable: whBalances.reduce((sum, b) => sum + b.availableUnits, 0),
              skuCount: whBalances.length,
              atRiskSkus: whBalances.filter((b) => b.reorderPoint != null && b.availableUnits < b.reorderPoint).length,
            };
          }),
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load warehouses');
        this.loading.set(false);
      },
    });
  }
}
