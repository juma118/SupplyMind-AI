import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { OperationsApiService, PurchaseOrder } from '../../core/api/operations-api.service';
import { NewPurchaseOrderDialogComponent } from './new-purchase-order-dialog.component';

@Component({
  selector: 'app-procurement',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="header-row">
      <h2>Procurement</h2>
      <button mat-raised-button color="primary" (click)="openNewOrder()">
        <mat-icon>add</mat-icon>
        New Purchase Order
      </button>
    </div>
    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <table mat-table [dataSource]="orders()" class="mat-elevation-z1">
        <ng-container matColumnDef="supplierId">
          <th mat-header-cell *matHeaderCellDef>Supplier</th>
          <td mat-cell *matCellDef="let po">{{ po.supplierId }}</td>
        </ng-container>
        <ng-container matColumnDef="items">
          <th mat-header-cell *matHeaderCellDef>Items</th>
          <td mat-cell *matCellDef="let po">{{ formatItems(po) }}</td>
        </ng-container>
        <ng-container matColumnDef="warehouseCode">
          <th mat-header-cell *matHeaderCellDef>Warehouse</th>
          <td mat-cell *matCellDef="let po">{{ po.warehouseCode }}</td>
        </ng-container>
        <ng-container matColumnDef="totalValue">
          <th mat-header-cell *matHeaderCellDef>Value</th>
          <td mat-cell *matCellDef="let po">{{ po.totalValue != null ? ('$' + po.totalValue) : '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="source">
          <th mat-header-cell *matHeaderCellDef>Source</th>
          <td mat-cell *matCellDef="let po">
            @if (po.sourceType === 'AI_RECOMMENDATION') {
              <mat-chip class="ai-chip">AI</mat-chip>
            } @else {
              -
            }
          </td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let po"><mat-chip [class]="statusClass(po.status)">{{ po.status }}</mat-chip></td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let po">
            @if (po.status === 'DRAFT') {
              <button mat-button color="primary" (click)="submit(po)">Submit</button>
            } @else if (po.status === 'SUBMITTED') {
              <button mat-button color="primary" (click)="receive(po)">Receive</button>
            }
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
      @if (orders().length === 0) {
        <p class="hint">No purchase orders yet.</p>
      }
    }
  `,
  styles: [`
    .header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .error { color: #b3261e; }
    .hint { color: rgba(0,0,0,0.6); }
    table { width: 100%; }
    .ai-chip { background: #e8def8; color: #4a3b78; }
  `],
})
export class ProcurementComponent implements OnInit {
  readonly columns = ['supplierId', 'items', 'warehouseCode', 'totalValue', 'source', 'status', 'actions'];
  readonly orders = signal<PurchaseOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly api: OperationsApiService,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
  ) {}

  openNewOrder(): void {
    this.dialog
      .open(NewPurchaseOrderDialogComponent)
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.load();
        }
      });
  }

  ngOnInit(): void {
    this.load();
  }

  formatItems(po: PurchaseOrder): string {
    return po.items.map((i) => `${i.quantity}x ${i.productSku}`).join(', ');
  }

  statusClass(status: string): string {
    if (status === 'RECEIVED') return 'risk-low';
    if (status === 'DRAFT') return '';
    return 'risk-medium';
  }

  submit(po: PurchaseOrder): void {
    this.api.submitPurchaseOrder(po.id).subscribe({
      next: () => {
        this.snackBar.open(`PO submitted`, 'Dismiss', { duration: 3000 });
        this.load();
      },
      error: (err) => this.snackBar.open(err.error?.message ?? 'Submit failed', 'Dismiss', { duration: 4000 }),
    });
  }

  receive(po: PurchaseOrder): void {
    this.api.receivePurchaseOrder(po.id).subscribe({
      next: () => {
        this.snackBar.open(`PO received - inventory updated`, 'Dismiss', { duration: 3000 });
        this.load();
      },
      error: (err) => this.snackBar.open(err.error?.message ?? 'Receive failed', 'Dismiss', { duration: 4000 }),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.listPurchaseOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load purchase orders');
        this.loading.set(false);
      },
    });
  }
}
