import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { OperationsApiService, StockoutRisk } from '../../core/api/operations-api.service';
import { DemandProfileDialogComponent } from './demand-profile-dialog.component';

@Component({
  selector: 'app-forecasting',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatChipsModule,
  ],
  template: `
    <div class="header-row">
      <h2>Forecasting &amp; Analytics</h2>
      <button mat-raised-button color="primary" (click)="openDemandProfile()">
        <mat-icon>add</mat-icon>
        Set Demand Profile
      </button>
    </div>
    <p class="notice">
      README §6's baseline: reorder point and safety stock are computed from a SKU's
      average daily demand and lead time (deterministic Java, not an LLM - README §5).
      No historical-sales forecasting model exists yet, so there's no demand curve to
      chart; what's below is real stockout-risk data for every SKU with a demand profile set.
    </p>

    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <table mat-table [dataSource]="risks()" class="mat-elevation-z1">
        <ng-container matColumnDef="productSku">
          <th mat-header-cell *matHeaderCellDef>SKU</th>
          <td mat-cell *matCellDef="let r">{{ r.productSku }}</td>
        </ng-container>
        <ng-container matColumnDef="warehouseCode">
          <th mat-header-cell *matHeaderCellDef>Warehouse</th>
          <td mat-cell *matCellDef="let r">{{ r.warehouseCode }}</td>
        </ng-container>
        <ng-container matColumnDef="availableUnits">
          <th mat-header-cell *matHeaderCellDef>Available</th>
          <td mat-cell *matCellDef="let r">{{ r.availableUnits }}</td>
        </ng-container>
        <ng-container matColumnDef="daysOfCover">
          <th mat-header-cell *matHeaderCellDef>Days of Cover</th>
          <td mat-cell *matCellDef="let r">{{ r.daysOfCover | number: '1.1-1' }}</td>
        </ng-container>
        <ng-container matColumnDef="reorderPoint">
          <th mat-header-cell *matHeaderCellDef>Reorder Point</th>
          <td mat-cell *matCellDef="let r">{{ r.reorderPoint ?? '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="risk">
          <th mat-header-cell *matHeaderCellDef>Risk</th>
          <td mat-cell *matCellDef="let r">
            @if (r.belowReorderPoint) {
              <mat-chip class="risk-high">Below reorder point</mat-chip>
            } @else if (r.coverShorterThanLeadTime) {
              <mat-chip class="risk-medium">Cover &lt; lead time</mat-chip>
            } @else {
              <mat-chip class="risk-low">OK</mat-chip>
            }
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
      @if (risks().length === 0) {
        <p class="hint">No SKU has a demand profile set yet - use "Set Demand Profile" above.</p>
      }
    }
  `,
  styles: [`
    .header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .notice { color: rgba(0,0,0,0.7); background: #fff4cc; padding: 0.75rem 1rem; border-radius: 4px; max-width: 760px; }
    .error { color: #b3261e; }
    .hint { color: rgba(0,0,0,0.6); margin-top: 1rem; }
    table { width: 100%; margin-top: 1rem; }
    .risk-high { background: #fde8e6; color: #b3261e; }
    .risk-medium { background: #fff4cc; color: #8a6d00; }
    .risk-low { background: #e3f3e6; color: #1e7b34; }
  `],
})
export class ForecastingComponent implements OnInit {
  readonly columns = ['productSku', 'warehouseCode', 'availableUnits', 'daysOfCover', 'reorderPoint', 'risk'];
  readonly risks = signal<StockoutRisk[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly api: OperationsApiService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  openDemandProfile(): void {
    this.dialog
      .open(DemandProfileDialogComponent)
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  private load(): void {
    this.loading.set(true);
    this.api.getStockoutRisk().subscribe({
      next: (risks) => {
        this.risks.set(risks);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load stockout risk data');
        this.loading.set(false);
      },
    });
  }
}
