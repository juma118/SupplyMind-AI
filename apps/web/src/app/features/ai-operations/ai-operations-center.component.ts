import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AiOperationsApiService, Investigation } from '../../core/api/ai-operations-api.service';

@Component({
  selector: 'app-ai-operations-center',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <h2>AI Operations Center</h2>
    <p class="hint">Live from the AI Operations Service - every row below is a real investigation.</p>

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
            <mat-chip>{{ inv.status }}</mat-chip>
          </td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef>Created</th>
          <td mat-cell *matCellDef="let inv">{{ inv.createdAt | date: 'short' }}</td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    }
  `,
  styles: [`
    .hint { color: rgba(0,0,0,0.6); margin-bottom: 1rem; }
    .error { color: #b3261e; }
    table { width: 100%; }
  `],
})
export class AiOperationsCenterComponent implements OnInit {
  readonly columns = ['sourceReference', 'incidentType', 'severity', 'status', 'createdAt'];
  readonly investigations = signal<Investigation[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(private readonly api: AiOperationsApiService) {}

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
