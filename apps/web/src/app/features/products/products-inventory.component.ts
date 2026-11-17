import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';
import { CoreApiService, Product } from '../../core/api/core-api.service';
import { InventoryBalance, OperationsApiService } from '../../core/api/operations-api.service';
import { NewProductDialogComponent } from './new-product-dialog.component';

interface ProductRow extends Product {
  totalAvailable: number;
  warehouseCount: number;
}

@Component({
  selector: 'app-products-inventory',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <div class="header-row">
      <h2>Products &amp; Inventory</h2>
      <button mat-raised-button color="primary" (click)="openNewProduct()">
        <mat-icon>add</mat-icon>
        New SKU
      </button>
    </div>

    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <table mat-table [dataSource]="rows()" class="mat-elevation-z1">
        <ng-container matColumnDef="sku">
          <th mat-header-cell *matHeaderCellDef>SKU</th>
          <td mat-cell *matCellDef="let p">{{ p.sku }}</td>
        </ng-container>
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Name</th>
          <td mat-cell *matCellDef="let p">{{ p.name }}</td>
        </ng-container>
        <ng-container matColumnDef="available">
          <th mat-header-cell *matHeaderCellDef>Available (all warehouses)</th>
          <td mat-cell *matCellDef="let p" [class.low-stock]="p.reorderPoint != null && p.totalAvailable < p.reorderPoint">
            {{ p.totalAvailable }}
          </td>
        </ng-container>
        <ng-container matColumnDef="reorderPoint">
          <th mat-header-cell *matHeaderCellDef>Reorder Point</th>
          <td mat-cell *matCellDef="let p">{{ p.reorderPoint ?? '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="unitCost">
          <th mat-header-cell *matHeaderCellDef>Unit Cost</th>
          <td mat-cell *matCellDef="let p">{{ p.unitCost != null ? ('$' + p.unitCost) : '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="warehouseCount">
          <th mat-header-cell *matHeaderCellDef>Warehouses</th>
          <td mat-cell *matCellDef="let p">{{ p.warehouseCount }}</td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
      @if (rows().length === 0) {
        <p class="hint">No products yet - create one with "New SKU", or run scripts/seed-data.sh.</p>
      }
    }
  `,
  styles: [`
    .header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .error { color: #b3261e; }
    .hint { color: rgba(0,0,0,0.6); }
    table { width: 100%; }
    .low-stock { color: #b3261e; font-weight: 600; }
  `],
})
export class ProductsInventoryComponent implements OnInit {
  readonly columns = ['sku', 'name', 'available', 'reorderPoint', 'unitCost', 'warehouseCount'];
  readonly rows = signal<ProductRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly coreApi: CoreApiService,
    private readonly operationsApi: OperationsApiService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  openNewProduct(): void {
    this.dialog
      .open(NewProductDialogComponent)
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.load();
        }
      });
  }

  private load(): void {
    this.loading.set(true);
    forkJoin({
      products: this.coreApi.listProducts(),
      balances: this.operationsApi.listInventory(),
    }).subscribe({
      next: ({ products, balances }) => {
        const balancesBySku = new Map<string, InventoryBalance[]>();
        for (const balance of balances) {
          const list = balancesBySku.get(balance.productSku) ?? [];
          list.push(balance);
          balancesBySku.set(balance.productSku, list);
        }
        this.rows.set(
          products.map((p) => {
            const productBalances = balancesBySku.get(p.sku) ?? [];
            return {
              ...p,
              totalAvailable: productBalances.reduce((sum, b) => sum + b.availableUnits, 0),
              warehouseCount: productBalances.length,
            };
          }),
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load products/inventory');
        this.loading.set(false);
      },
    });
  }
}
