import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { CoreApiService, SupplierPerformance } from '../../core/api/core-api.service';

@Component({
  selector: 'app-suppliers',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <h2>Suppliers</h2>
    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <table mat-table [dataSource]="suppliers()" class="mat-elevation-z1">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Supplier</th>
          <td mat-cell *matCellDef="let s">{{ s.name }}</td>
        </ng-container>
        <ng-container matColumnDef="onTimeRate">
          <th mat-header-cell *matHeaderCellDef>On-Time %</th>
          <td mat-cell *matCellDef="let s">{{ s.onTimeRate != null ? (s.onTimeRate * 100).toFixed(1) + '%' : '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="avgLeadTimeDays">
          <th mat-header-cell *matHeaderCellDef>Lead Time</th>
          <td mat-cell *matCellDef="let s">{{ s.avgLeadTimeDays != null ? s.avgLeadTimeDays + 'd' : '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="risk">
          <!-- Risk label comes from Core Service's /suppliers/{id}/performance (SupplierPerformanceResponse),
               not recomputed here - the threshold logic has exactly one home. -->
          <th mat-header-cell *matHeaderCellDef>Risk</th>
          <td mat-cell *matCellDef="let s">
            <mat-chip [class]="'risk-' + s.riskLevel.toLowerCase()">{{ s.riskLevel }}</mat-chip>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
      @if (suppliers().length === 0) {
        <p class="hint">No suppliers yet - run scripts/seed-data.sh or create one via the Core Service API.</p>
      }
    }
  `,
  styles: [`
    .error { color: #b3261e; }
    .hint { color: rgba(0,0,0,0.6); }
    table { width: 100%; }
    .risk-low { background: #e3f3e6; color: #1e7b34; }
    .risk-medium { background: #fff4cc; color: #8a6d00; }
    .risk-high { background: #fde8e6; color: #b3261e; }
    .risk-unknown { background: #eceff1; color: #546e7a; }
  `],
})
export class SuppliersComponent implements OnInit {
  readonly columns = ['name', 'onTimeRate', 'avgLeadTimeDays', 'risk'];
  readonly suppliers = signal<SupplierPerformance[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(private readonly coreApi: CoreApiService) {}

  ngOnInit(): void {
    this.coreApi
      .listSuppliers()
      .pipe(
        switchMap((suppliers) => {
          if (suppliers.length === 0) {
            return of([]);
          }
          return forkJoin(
            suppliers.map((s) =>
              this.coreApi.getSupplierPerformance(s.id).pipe(
                catchError(() =>
                  of<SupplierPerformance>({
                    supplierId: s.id,
                    name: s.name,
                    onTimeRate: s.onTimeRate,
                    avgLeadTimeDays: s.avgLeadTimeDays,
                    riskLevel: 'UNKNOWN',
                  }),
                ),
              ),
            ),
          );
        }),
      )
      .subscribe({
        next: (performances) => {
          this.suppliers.set(performances);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.message ?? 'Could not load suppliers');
          this.loading.set(false);
        },
      });
  }
}
