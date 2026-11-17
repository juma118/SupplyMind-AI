import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Shipment, ShipmentApiService, ShipmentEvent } from '../../core/api/shipment-api.service';
import { NewShipmentDialogComponent } from './new-shipment-dialog.component';

@Component({
  selector: 'app-shipments',
  standalone: true,
  imports: [
    CommonModule,
    MatExpansionModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatButtonToggleModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
  ],
  template: `
    <div class="header-row">
      <h2>Shipments</h2>
      <div class="header-actions">
        <mat-button-toggle-group [value]="filter()" (change)="setFilter($event.value)">
          <mat-button-toggle value="all">All</mat-button-toggle>
          <mat-button-toggle value="at-risk">At Risk</mat-button-toggle>
        </mat-button-toggle-group>
        <button mat-raised-button color="primary" (click)="openNewShipment()">
          <mat-icon>add</mat-icon>
          New Shipment
        </button>
      </div>
    </div>

    @if (loading()) {
      <mat-spinner diameter="32" />
    } @else if (error()) {
      <p class="error">{{ error() }}</p>
    } @else {
      <mat-accordion>
        @for (shipment of shipments(); track shipment.id) {
          <mat-expansion-panel (opened)="loadTimeline(shipment.id)">
            <mat-expansion-panel-header>
              <mat-panel-title>{{ shipment.shipmentCode }} ({{ shipment.carrier ?? 'unknown carrier' }})</mat-panel-title>
              <mat-panel-description>
                {{ shipment.origin ?? '?' }} &rarr; {{ shipment.destinationWarehouse }}
                <mat-chip [class.chip-delayed]="shipment.status === 'DELAYED'">{{ shipment.status }}</mat-chip>
              </mat-panel-description>
            </mat-expansion-panel-header>

            <p>{{ shipment.quantity }} units of {{ shipment.productSku }}</p>

            @if (timelines()[shipment.id]) {
              <div class="timeline">
                @for (event of timelines()[shipment.id]; track event.id) {
                  <div class="timeline-row">
                    <strong>{{ event.eventType }}</strong>
                    <span>{{ event.occurredAt | date: 'short' }}</span>
                    @if (event.description) {
                      <span class="desc">{{ event.description }}</span>
                    }
                    @if (event.delayHours) {
                      <span class="desc">+{{ event.delayHours }}h</span>
                    }
                  </div>
                }
              </div>
            } @else {
              <mat-spinner diameter="20" />
            }
          </mat-expansion-panel>
        }
      </mat-accordion>
      @if (shipments().length === 0) {
        <p class="hint">No shipments yet.</p>
      }
    }
  `,
  styles: [`
    .header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .header-actions { display: flex; align-items: center; gap: 1rem; }
    .error { color: #b3261e; }
    .hint { color: rgba(0,0,0,0.6); }
    .chip-delayed { background: #fde8e6; color: #b3261e; }
    .timeline { display: flex; flex-direction: column; gap: 0.4rem; padding: 0.75rem 0; }
    .timeline-row { display: flex; gap: 0.75rem; align-items: baseline; }
    .desc { color: rgba(0,0,0,0.6); }
  `],
})
export class ShipmentsComponent implements OnInit {
  readonly shipments = signal<Shipment[]>([]);
  readonly timelines = signal<Record<string, ShipmentEvent[]>>({});
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filter = signal<'all' | 'at-risk'>('all');

  constructor(
    private readonly api: ShipmentApiService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  openNewShipment(): void {
    this.dialog
      .open(NewShipmentDialogComponent)
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.load();
        }
      });
  }

  setFilter(value: 'all' | 'at-risk'): void {
    this.filter.set(value);
    this.load();
  }

  loadTimeline(shipmentId: string): void {
    if (this.timelines()[shipmentId]) {
      return;
    }
    this.api.getTimeline(shipmentId).subscribe((events) => {
      this.timelines.update((current) => ({ ...current, [shipmentId]: events }));
    });
  }

  private load(): void {
    this.loading.set(true);
    const source = this.filter() === 'at-risk' ? this.api.listAtRisk() : this.api.listShipments();
    source.subscribe({
      next: (shipments) => {
        this.shipments.set(shipments);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'Could not load shipments');
        this.loading.set(false);
      },
    });
  }
}
