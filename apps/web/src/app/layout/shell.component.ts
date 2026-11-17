import { CommonModule } from '@angular/common';
import { Component, computed } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../core/auth/auth.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  /** Roles allowed to see this link (platform-admin always included) - undefined = everyone. */
  roles?: string[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatSidenavModule,
    MatButtonModule,
    MatMenuModule,
  ],
  template: `
    <mat-toolbar color="primary">
      <span>SupplyMind AI</span>
      <span class="spacer"></span>
      @if (auth.user(); as user) {
        <span class="tenant-badge">{{ user.tenantId }}</span>
        <button mat-button [matMenuTriggerFor]="userMenu">
          <mat-icon>account_circle</mat-icon>
          {{ user.name }}
        </button>
        <mat-menu #userMenu="matMenu">
          <div class="roles-label">{{ user.roles.join(', ') || 'no roles' }}</div>
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon>
            Log out
          </button>
        </mat-menu>
      }
    </mat-toolbar>
    <mat-sidenav-container class="body-container">
      <mat-sidenav mode="side" opened class="sidenav">
        <mat-nav-list>
          @for (item of visibleNavItems(); track item.path) {
            <a mat-list-item [routerLink]="item.path" routerLinkActive="active-link">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content class="content">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .spacer { flex: 1 1 auto; }
    .tenant-badge { font-size: 0.85rem; opacity: 0.85; margin-right: 1rem; }
    .roles-label { padding: 0.5rem 1rem; color: rgba(0,0,0,0.6); font-size: 0.85rem; }
    .body-container { height: calc(100vh - 64px); }
    .sidenav { width: 240px; }
    .content { padding: 1.5rem; }
    .active-link { background: rgba(0,0,0,0.06); }
  `],
})
export class ShellComponent {
  // Mirrors the roleGuard() assignments in app.routes.ts - keep these in sync; a link
  // shown here for a role that the route guard then rejects would just be confusing.
  private readonly navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard' },
    { label: 'Products & Inventory', path: '/inventory', icon: 'inventory_2', roles: ['warehouse-manager'] },
    { label: 'Warehouses', path: '/warehouses', icon: 'warehouse', roles: ['warehouse-manager'] },
    { label: 'Shipments', path: '/shipments', icon: 'local_shipping', roles: ['logistics-coordinator'] },
    { label: 'Suppliers', path: '/suppliers', icon: 'storefront', roles: ['procurement-manager'] },
    { label: 'Procurement', path: '/procurement', icon: 'receipt_long', roles: ['procurement-manager'] },
    { label: 'AI Operations', path: '/ai-operations', icon: 'smart_toy', roles: ['supply-chain-manager'] },
    { label: 'Approvals', path: '/approvals', icon: 'task_alt', roles: ['supply-chain-manager'] },
    { label: 'Forecasting', path: '/forecasting', icon: 'insights', roles: ['operations-analyst'] },
    { label: 'Audit', path: '/audit', icon: 'history', roles: ['auditor'] },
  ];

  readonly visibleNavItems = computed(() => {
    const user = this.auth.user();
    return this.navItems.filter((item) => {
      if (!item.roles) return true;
      if (!user) return false;
      return user.roles.includes('platform-admin') || item.roles.some((r) => user.roles.includes(r));
    });
  });

  constructor(readonly auth: AuthService) {}

  logout(): void {
    this.auth.logout();
  }
}
