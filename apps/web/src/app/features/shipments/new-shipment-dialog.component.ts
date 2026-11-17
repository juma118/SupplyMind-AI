import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CoreApiService, Warehouse } from '../../core/api/core-api.service';
import { ShipmentApiService } from '../../core/api/shipment-api.service';

@Component({
  selector: 'app-new-shipment-dialog',
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
    <h2 mat-dialog-title>New Shipment</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Shipment code</mat-label>
        <input matInput [(ngModel)]="shipmentCode" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Carrier</mat-label>
        <input matInput [(ngModel)]="carrier" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Origin</mat-label>
        <input matInput [(ngModel)]="origin" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Destination warehouse</mat-label>
        <mat-select [(ngModel)]="destinationWarehouse" required>
          @for (wh of warehouses(); track wh.id) {
            <mat-option [value]="wh.code">{{ wh.code }} - {{ wh.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Product SKU</mat-label>
        <input matInput [(ngModel)]="productSku" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Quantity</mat-label>
        <input matInput type="number" [(ngModel)]="quantity" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Avg daily demand (units/day)</mat-label>
        <input matInput type="number" [(ngModel)]="avgDailyDemand" />
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
        [disabled]="!shipmentCode || !destinationWarehouse || !productSku || !quantity || saving()"
        (click)="save()"
      >
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { width: 100%; }
    .error { color: #b3261e; }
  `],
})
export class NewShipmentDialogComponent implements OnInit {
  shipmentCode = '';
  carrier = '';
  origin = '';
  destinationWarehouse = '';
  productSku = '';
  quantity: number | null = null;
  avgDailyDemand: number | null = null;
  readonly warehouses = signal<Warehouse[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor(
    public readonly dialogRef: MatDialogRef<NewShipmentDialogComponent>,
    private readonly coreApi: CoreApiService,
    private readonly shipmentApi: ShipmentApiService,
  ) {}

  ngOnInit(): void {
    this.coreApi.listWarehouses().subscribe((warehouses) => this.warehouses.set(warehouses));
  }

  save(): void {
    this.saving.set(true);
    this.shipmentApi
      .createShipment({
        shipmentCode: this.shipmentCode,
        carrier: this.carrier || undefined,
        origin: this.origin || undefined,
        destinationWarehouse: this.destinationWarehouse,
        productSku: this.productSku,
        quantity: this.quantity!,
        avgDailyDemand: this.avgDailyDemand ?? undefined,
      })
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => {
          this.error.set(err.error?.message ?? err.message ?? 'Could not create shipment');
          this.saving.set(false);
        },
      });
  }
}
