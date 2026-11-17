import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiOperationsApiService, Investigation } from '../../core/api/ai-operations-api.service';
import { InvestigationDetailDialogComponent } from './investigation-detail-dialog.component';

@Component({
  selector: 'app-ai-operations-center',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatChipsModule, MatIconModule, MatDialogModule, MatProgressSpinnerModule],
  template: `
    <div class="header-row">
      <h2 class="ai-gradient-text">AI Operations Center</h2>
    </div>
    <p class="hint">
      Live from the AI Operations Service - every row is a real investigation run by the agent
      pipeline (Shipment/Inventory/Supplier Intelligence &rarr; Coordinator &rarr; Policy Retrieval
      &rarr; Recommendation Evaluator). Click a row to see the full agent trace.
    </p>

    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">Could not load investigations: {{ error() }}</p>
    } @else {
      <table mat-table [dataSource]="investigations()" class="mat-elevation-z1">
        <ng-container matColumnDef="sourceReference">
          <th mat-header-cell *matHeaderCellDef>Shipment</th>
          <td mat-cell *matCellDef="let inv">{{ inv.sourceReference }}</td>
        </ng-container>
        <ng-container matColumnDef="incidentType">
          <th mat-header-cell *matHeaderCellDef>Type</th>
          <td mat-cell *matCellDef="let inv">{{ inv.incidentType }}</td>
        </ng-container>
        <ng-container matColumnDef="severity">
          <th mat-header-cell *matHeaderCellDef>Severity</th>
          <td mat-cell *matCellDef="let inv">{{ inv.severity ?? '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let inv">
            <mat-chip [class]="statusClass(inv.status)">{{ inv.status }}</mat-chip>
          </td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef>Created</th>
          <td mat-cell *matCellDef="let inv">{{ inv.createdAt | date: 'short' }}</td>
        </ng-container>
        <ng-container matColumnDef="trace">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let inv">
            <mat-icon class="trace-icon">chevron_right</mat-icon>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns" class="clickable-row" (click)="openTrace(row)"></tr>
      </table>
      @if (investigations().length === 0) {
        <p class="hint">No investigations yet - one opens automatically when a shipment delay event arrives.</p>
      }
    }
  `,
  styles: [`
    .header-row { margin-bottom: 0.25rem; }
    .hint { color: var(--sm-text-secondary); margin-bottom: 1rem; max-width: 760px; }
    .error { color: #b3261e; }
    table { width: 100%; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover { background: var(--sm-ai-grad-soft); }
    .trace-icon { color: var(--sm-text-secondary); }
  `],
})
export class AiOperationsCenterComponent implements OnInit {
  readonly columns = ['sourceReference', 'incidentType', 'severity', 'status', 'createdAt', 'trace'];
  readonly investigations = signal<Investigation[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly api: AiOperationsApiService,
    private readonly dialog: MatDialog,
  ) {}

  statusClass(status: string): string {
    if (status === 'CLOSED') return 'risk-low';
    if (status === 'RECEIVED' || status === 'INVESTIGATING') return 'ai-chip';
    return '';
  }

  openTrace(investigation: Investigation): void {
    this.dialog.open(InvestigationDetailDialogComponent, {
      width: '640px',
      maxHeight: '85vh',
      data: { investigationId: investigation.id },
    });
  }

  ngOnInit(): void {
    this.api.listInvestigations().subscribe({
      next: (investigations) => {
        this.investigations.set(investigations);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'unknown error');
        this.loading.set(false);
      },
    });
  }
}
