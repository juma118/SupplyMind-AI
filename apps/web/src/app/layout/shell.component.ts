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
  /** Gets the AI-gradient icon treatment - only screens actually driven by agents/models. */
  aiPowered?: boolean;
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
    <mat-toolbar color="primary" class="toolbar">
      <span class="brand-badge"><mat-icon>hub</mat-icon></span>
      <span class="brand">SupplyMind<span class="brand-ai">&nbsp;AI</span></span>
      <span class="spacer"></span>
      @if (auth.user(); as user) {
        <span class="tenant-badge">{{ user.tenantId }}</span>
        <button mat-button [matMenuTriggerFor]="userMenu" class="user-menu-trigger">
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
            <a mat-list-item [routerLink]="item.path" routerLinkActive="active-link" [class.ai-nav-item]="item.aiPowered">
              @if (item.aiPowered) {
                <span matListItemIcon class="ai-nav-icon"><mat-icon>{{ item.icon }}</mat-icon></span>
              } @else {
                <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              }
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
    .toolbar { position: sticky; top: 0; z-index: 10; box-shadow: 0 1px 4px rgba(0,0,0,0.12); }
    .brand-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 9px;
      margin-right: 0.6rem;
      background: var(--sm-ai-grad);
    }
    .brand-badge mat-icon { color: #fff; font-size: 20px; width: 20px; height: 20px; }
    .brand { font-weight: 600; letter-spacing: -0.01em; font-size: 1.1rem; }
    .brand-ai { opacity: 0.85; }
    .spacer { flex: 1 1 auto; }
    .tenant-badge {
      font-size: 0.75rem;
      opacity: 0.9;
      margin-right: 1rem;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      background: rgba(255,255,255,0.15);
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }
    .roles-label { padding: 0.5rem 1rem; color: rgba(0,0,0,0.6); font-size: 0.85rem; }
    .body-container { height: calc(100vh - 64px); }
    .sidenav {
      width: 248px;
      background: var(--sm-surface);
      border-right: 1px solid var(--sm-border);
      padding-top: 0.5rem;
    }
    .content { padding: 1.5rem; }
    mat-nav-list a {
      margin: 0.15rem 0.5rem;
      border-radius: 8px;
    }
    .active-link {
      background: rgba(26, 35, 126, 0.08);
      color: #1a237e;
      font-weight: 500;
    }
    .active-link mat-icon { color: #1a237e; }
    .ai-nav-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border-radius: 8px;
      background: var(--sm-ai-grad-soft);
    }
    .ai-nav-icon mat-icon { color: #6d28d9; font-size: 18px; width: 18px; height: 18px; }
    .ai-nav-item.active-link .ai-nav-icon { background: var(--sm-ai-grad); }
    .ai-nav-item.active-link .ai-nav-icon mat-icon { color: #fff; }
    .ai-nav-item.active-link { color: #6d28d9; }
  `],
})
export class ShellComponent {
  // Mirrors the roleGuard() assignments in app.routes.ts - keep these in sync; a link
  // shown here for a role that the route guard then rejects would just be confusing.
  // aiPowered marks screens that are actually agent/model-driven (vs. deterministic CRUD
  // or rule-based analytics) - only those get the gradient treatment, so the AI visual
  // language stays meaningful instead of decorating everything.
  private readonly navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard' },
    { label: 'Products & Inventory', path: '/inventory', icon: 'inventory_2', roles: ['warehouse-manager'] },
    { label: 'Warehouses', path: '/warehouses', icon: 'warehouse', roles: ['warehouse-manager'] },
    { label: 'Shipments', path: '/shipments', icon: 'local_shipping', roles: ['logistics-coordinator'] },
    { label: 'Suppliers', path: '/suppliers', icon: 'storefront', roles: ['procurement-manager'] },
    { label: 'Procurement', path: '/procurement', icon: 'receipt_long', roles: ['procurement-manager'] },
    { label: 'AI Operations', path: '/ai-operations', icon: 'smart_toy', roles: ['supply-chain-manager'], aiPowered: true },
    { label: 'Approvals', path: '/approvals', icon: 'task_alt', roles: ['supply-chain-manager'], aiPowered: true },
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
