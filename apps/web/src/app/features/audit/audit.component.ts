import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { AiOperationsApiService, AuditLogEntry } from '../../core/api/ai-operations-api.service';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatProgressSpinnerModule],
  template: `
    <h2>Audit Log</h2>
    <p class="hint">
      Every row here is an immutable record from the AI Operations Service - Kafka ingest, agent
      recommendations, and human approve/reject decisions, in one trail.
    </p>

    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <table mat-table [dataSource]="logs()" class="mat-elevation-z1">
        <ng-container matColumnDef="occurredAt">
          <th mat-header-cell *matHeaderCellDef>Time</th>
          <td mat-cell *matCellDef="let log">{{ log.occurredAt | date: 'short' }}</td>
        </ng-container>
        <ng-container matColumnDef="actor">
          <th mat-header-cell *matHeaderCellDef>Actor</th>
          <td mat-cell *matCellDef="let log">{{ log.actor }}</td>
        </ng-container>
        <ng-container matColumnDef="action">
          <th mat-header-cell *matHeaderCellDef>Action</th>
          <td mat-cell *matCellDef="let log">{{ log.action }}</td>
        </ng-container>
        <ng-container matColumnDef="entity">
          <th mat-header-cell *matHeaderCellDef>Entity</th>
          <td mat-cell *matCellDef="let log">{{ log.entityType }} / {{ log.entityId | slice: 0 : 8 }}</td>
        </ng-container>
        <ng-container matColumnDef="details">
          <th mat-header-cell *matHeaderCellDef>Details</th>
          <td mat-cell *matCellDef="let log">{{ log.details }}</td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
      @if (logs().length === 0) {
        <p class="hint">No audit events yet.</p>
      }
    }
  `,
  styles: [`
    .hint { color: rgba(0,0,0,0.6); margin-bottom: 1rem; }
    .error { color: #b3261e; }
    table { width: 100%; }
    td { max-width: 320px; overflow: hidden; text-overflow: ellipsis; }
  `],
})
export class AuditComponent implements OnInit {
  readonly columns = ['occurredAt', 'actor', 'action', 'entity', 'details'];
  readonly logs = signal<AuditLogEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(private readonly api: AiOperationsApiService) {}

  ngOnInit(): void {
    this.api.listAuditLogs().subscribe({
      next: (logs) => {
        this.logs.set(logs);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load audit log');
        this.loading.set(false);
      },
    });
  }
}
