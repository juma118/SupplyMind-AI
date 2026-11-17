import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CoreApiService } from '../../core/api/core-api.service';

@Component({
  selector: 'app-new-product-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>New SKU</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>SKU</mat-label>
        <input matInput [(ngModel)]="sku" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Name</mat-label>
        <input matInput [(ngModel)]="name" required />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Unit cost</mat-label>
        <input matInput type="number" [(ngModel)]="unitCost" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Reorder point</mat-label>
        <input matInput type="number" [(ngModel)]="reorderPoint" />
      </mat-form-field>
      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!sku || !name || saving()" (click)="save()">
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { width: 100%; }
    .error { color: #b3261e; }
  `],
})
export class NewProductDialogComponent {
  sku = '';
  name = '';
  unitCost: number | null = null;
  reorderPoint: number | null = null;
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor(
    public readonly dialogRef: MatDialogRef<NewProductDialogComponent>,
    private readonly coreApi: CoreApiService,
  ) {}

  save(): void {
    this.saving.set(true);
    this.coreApi
      .createProduct({
        sku: this.sku,
        name: this.name,
        unitCost: this.unitCost ?? undefined,
        reorderPoint: this.reorderPoint ?? undefined,
      })
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err) => {
          this.error.set(err.error?.message ?? err.message ?? 'Could not create product');
          this.saving.set(false);
        },
      });
  }
}
