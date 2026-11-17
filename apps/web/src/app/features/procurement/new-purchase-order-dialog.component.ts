import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { CoreApiService, Supplier, Warehouse } from '../../core/api/core-api.service';
import { OperationsApiService } from '../../core/api/operations-api.service';

interface ItemRow {
  productSku: string;
  quantity: number | null;
  unitPrice: number | null;
}

@Component({
  selector: 'app-new-purchase-order-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>New Purchase Order</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Supplier</mat-label>
        <mat-select [(ngModel)]="supplierId" required>
          @for (s of suppliers(); track s.id) {
            <mat-option [value]="s.id">{{ s.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Destination warehouse</mat-label>
        <mat-select [(ngModel)]="warehouseCode" required>
          @for (wh of warehouses(); track wh.id) {
            <mat-option [value]="wh.code">{{ wh.code }} - {{ wh.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <h3>Items</h3>
      @for (item of items; track $index) {
        <div class="item-row">
          <mat-form-field appearance="outline">
            <mat-label>SKU</mat-label>
            <input matInput [(ngModel)]="item.productSku" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Qty</mat-label>
            <input matInput type="number" [(ngModel)]="item.quantity" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Unit price</mat-label>
            <input matInput type="number" [(ngModel)]="item.unitPrice" />
          </mat-form-field>
          <button mat-icon-button (click)="removeItem($index)" [disabled]="items.length === 1">
            <mat-icon>delete</mat-icon>
          </button>
        </div>
      }
      <button mat-button (click)="addItem()">
        <mat-icon>add</mat-icon>
        Add item
      </button>

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!canSave() || saving()" (click)="save()">
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { width: 100%; }
    .item-row { display: flex; gap: 0.5rem; align-items: center; }
    .item-row mat-form-field { flex: 1; }
    .error { color: #b3261e; }
  `],
})
export class NewPurchaseOrderDialogComponent implements OnInit {
  supplierId = '';
  warehouseCode = '';
  items: ItemRow[] = [{ productSku: '', quantity: null, unitPrice: null }];
  readonly suppliers = signal<Supplier[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor(
    public readonly dialogRef: MatDialogRef<NewPurchaseOrderDialogComponent>,
    private readonly coreApi: CoreApiService,
    private readonly operationsApi: OperationsApiService,
  ) {}

  ngOnInit(): void {
    forkJoin({
      suppliers: this.coreApi.listSuppliers(),
      warehouses: this.coreApi.listWarehouses(),
    }).subscribe(({ suppliers, warehouses }) => {
      this.suppliers.set(suppliers);
      this.warehouses.set(warehouses);
    });
  }

  addItem(): void {
    this.items.push({ productSku: '', quantity: null, unitPrice: null });
  }

  removeItem(index: number): void {
    this.items.splice(index, 1);
  }

  canSave(): boolean {
    return (
      !!this.supplierId &&
      !!this.warehouseCode &&
      this.items.every((i) => i.productSku && i.quantity != null && i.unitPrice != null)
    );
  }

  save(): void {
    this.saving.set(true);
    this.operationsApi
      .createPurchaseOrder({
        supplierId: this.supplierId,
        warehouseCode: this.warehouseCode,
        items: this.items.map((i) => ({
          productSku: i.productSku,
          quantity: i.quantity!,
          unitPrice: i.unitPrice!,
        })),
        sourceType: 'MANUAL',
      })
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => {
          this.error.set(err.error?.message ?? err.message ?? 'Could not create purchase order');
          this.saving.set(false);
        },
      });
  }
}
