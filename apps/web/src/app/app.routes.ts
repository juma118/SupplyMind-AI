import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        // Everyone - PRODUCT_SPEC.md §3 lists Dashboard under both Supply Chain Manager
        // and Operations Analyst, and it's the landing page for all other roles too.
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'inventory',
        canActivate: [roleGuard(['warehouse-manager'])],
        loadComponent: () =>
          import('./features/products/products-inventory.component').then((m) => m.ProductsInventoryComponent),
      },
      {
        path: 'warehouses',
        canActivate: [roleGuard(['warehouse-manager'])],
        loadComponent: () => import('./features/warehouses/warehouses.component').then((m) => m.WarehousesComponent),
      },
      {
        path: 'shipments',
        canActivate: [roleGuard(['logistics-coordinator'])],
        loadComponent: () => import('./features/shipments/shipments.component').then((m) => m.ShipmentsComponent),
      },
      {
        path: 'suppliers',
        canActivate: [roleGuard(['procurement-manager'])],
        loadComponent: () => import('./features/suppliers/suppliers.component').then((m) => m.SuppliersComponent),
      },
      {
        path: 'procurement',
        canActivate: [roleGuard(['procurement-manager'])],
        loadComponent: () =>
          import('./features/procurement/procurement.component').then((m) => m.ProcurementComponent),
      },
      {
        path: 'ai-operations',
        canActivate: [roleGuard(['supply-chain-manager'])],
        loadComponent: () =>
          import('./features/ai-operations/ai-operations-center.component').then(
            (m) => m.AiOperationsCenterComponent,
          ),
      },
      {
        path: 'approvals',
        canActivate: [roleGuard(['supply-chain-manager'])],
        loadComponent: () => import('./features/approvals/approvals.component').then((m) => m.ApprovalsComponent),
      },
      {
        path: 'forecasting',
        canActivate: [roleGuard(['operations-analyst'])],
        loadComponent: () =>
          import('./features/forecasting/forecasting.component').then((m) => m.ForecastingComponent),
      },
      {
        path: 'audit',
        canActivate: [roleGuard(['auditor'])],
        loadComponent: () => import('./features/audit/audit.component').then((m) => m.AuditComponent),
      },
    ],
  },
];
