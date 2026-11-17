import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { CoreApiService, Product, Warehouse } from '../../core/api/core-api.service';
import { OperationsApiService } from '../../core/api/operations-api.service';

@Component({
  selector: 'app-demand-profile-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Set Demand Profile</h2>
    <mat-dialog-content>
      <p class="hint">
        README §6: ROP = (Average Daily Demand &times; Lead Time) + Safety Stock. Setting
        this computes and stores the reorder point for real - it was never wired up before.
      </p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Warehouse</mat-label>
        <mat-select [(ngModel)]="warehouseCode" required>
          @for (wh of warehouses(); track wh.id) {
            <mat-option [value]="wh.code">{{ wh.code }} - {{ wh.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Product SKU</mat-label>
        <mat-select [(ngModel)]="productSku" required>
          @for (p of products(); track p.id) {
            <mat-option [value]="p.sku">{{ p.sku }} - {{ p.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Average daily demand (units/day)</mat-label>
        <input matInput type="number" [(ngModel)]="avgDailyDemand" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Lead time (days)</mat-label>
        <input matInput type="number" [(ngModel)]="leadTimeDays" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Safety stock override (optional - blank uses 0)</mat-label>
        <input matInput type="number" [(ngModel)]="safetyStockOverride" />
      </mat-form-field>
      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Cancel</button>
      <button
        mat-raised-button
        color="primary"
        [disabled]="!warehouseCode || !productSku || !avgDailyDemand || !leadTimeDays || saving()"
        (click)="save()"
      >
        Calculate &amp; Save
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { width: 100%; }
    .hint { color: rgba(0,0,0,0.6); font-size: 0.9rem; }
    .error { color: #b3261e; }
  `],
})
export class DemandProfileDialogComponent implements OnInit {
  warehouseCode = '';
  productSku = '';
  avgDailyDemand: number | null = null;
  leadTimeDays: number | null = null;
  safetyStockOverride: number | null = null;
  readonly warehouses = signal<Warehouse[]>([]);
  readonly products = signal<Product[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor(
    public readonly dialogRef: MatDialogRef<DemandProfileDialogComponent>,
    private readonly coreApi: CoreApiService,
    private readonly operationsApi: OperationsApiService,
  ) {}

  ngOnInit(): void {
    forkJoin({
      warehouses: this.coreApi.listWarehouses(),
      products: this.coreApi.listProducts(),
    }).subscribe(({ warehouses, products }) => {
      this.warehouses.set(warehouses);
      this.products.set(products);
    });
  }

  save(): void {
    this.saving.set(true);
    this.operationsApi
      .setDemandProfile({
        warehouseCode: this.warehouseCode,
        productSku: this.productSku,
        avgDailyDemand: this.avgDailyDemand!,
        leadTimeDays: this.leadTimeDays!,
        safetyStockOverride: this.safetyStockOverride ?? undefined,
      })
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => {
          this.error.set(err.error?.message ?? err.message ?? 'Could not save demand profile');
          this.saving.set(false);
        },
      });
  }
}
