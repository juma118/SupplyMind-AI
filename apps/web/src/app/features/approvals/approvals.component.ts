import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AiOperationsApiService, Recommendation } from '../../core/api/ai-operations-api.service';
import { InvestigationDetailDialogComponent } from '../ai-operations/investigation-detail-dialog.component';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 class="ai-gradient-text">Pending Approvals</h2>
    <p class="hint">Every card here is a real AI-generated recommendation awaiting a human decision.</p>

    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <div class="list">
        @for (rec of recommendations(); track rec.id) {
          <mat-card>
            <mat-card-content>
              <div class="action-row">
                <mat-chip class="ai-chip">{{ rec.actionType }}</mat-chip>
                @if (rec.estimatedCost != null) {
                  <span class="cost">Est. cost: {{ '$' + rec.estimatedCost }}</span>
                }
                <span class="spacer"></span>
                <button mat-button class="trace-link" (click)="openTrace(rec)">
                  <mat-icon>smart_toy</mat-icon>
                  View agent trace
                </button>
              </div>
              <p class="summary">{{ rec.summary }}</p>
              @if (rec.actionType === 'TRANSFER') {
                <p class="action-detail">
                  Transfer {{ rec.actionQuantity }} units of {{ rec.actionProductSku }} from
                  {{ rec.actionSourceWarehouse }} to {{ rec.actionDestWarehouse }}
                </p>
              } @else if (rec.actionType === 'PURCHASE_ORDER') {
                <p class="action-detail">
                  Purchase order: {{ rec.actionQuantity }} units of {{ rec.actionProductSku }} to
                  {{ rec.actionDestWarehouse }}, supplier {{ rec.actionSupplierId }}
                </p>
              }

              <mat-form-field appearance="outline" class="comment-field">
                <mat-label>Comment</mat-label>
                <input matInput [(ngModel)]="comments[rec.id]" placeholder="Optional" />
              </mat-form-field>

              <div class="actions">
                <button mat-raised-button color="primary" [disabled]="busy()" (click)="approve(rec)">
                  Approve
                </button>
                <button mat-stroked-button color="warn" [disabled]="busy()" (click)="reject(rec)">
                  Reject
                </button>
              </div>
            </mat-card-content>
          </mat-card>
        }
      </div>
      @if (recommendations().length === 0) {
        <p class="hint">Nothing pending right now.</p>
      }
    }
  `,
  styles: [`
    .hint { color: rgba(0,0,0,0.6); margin-bottom: 1rem; }
    .error { color: #b3261e; }
    .list { display: flex; flex-direction: column; gap: 1rem; max-width: 640px; }
    .action-row { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
    .spacer { flex: 1 1 auto; }
    .trace-link { color: #6d28d9; font-size: 0.85rem; }
    .cost { color: rgba(0,0,0,0.6); font-size: 0.9rem; }
    .summary { margin: 0.25rem 0; }
    .action-detail { color: rgba(0,0,0,0.7); font-size: 0.9rem; }
    .comment-field { width: 100%; margin-top: 0.5rem; }
    .actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
  `],
})
export class ApprovalsComponent implements OnInit {
  readonly recommendations = signal<Recommendation[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly comments: Record<string, string> = {};

  constructor(
    private readonly api: AiOperationsApiService,
    private readonly snackBar: MatSnackBar,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  openTrace(rec: Recommendation): void {
    this.dialog.open(InvestigationDetailDialogComponent, {
      width: '640px',
      maxHeight: '85vh',
      data: { investigationId: rec.investigationId },
    });
  }

  approve(rec: Recommendation): void {
    this.decide(rec, true);
  }

  reject(rec: Recommendation): void {
    this.decide(rec, false);
  }

  private decide(rec: Recommendation, approved: boolean): void {
    this.busy.set(true);
    const comment = this.comments[rec.id] ?? '';
    const call = approved ? this.api.approve(rec.id, comment) : this.api.reject(rec.id, comment);

    call.subscribe({
      next: () => {
        this.snackBar.open(approved ? 'Approved and executed' : 'Rejected', 'Dismiss', { duration: 3000 });
        this.busy.set(false);
        this.load();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message ?? 'Action failed', 'Dismiss', { duration: 4000 });
        this.busy.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.listPendingRecommendations().subscribe({
      next: (recommendations) => {
        this.recommendations.set(recommendations);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load approvals');
        this.loading.set(false);
      },
    });
  }
}
