import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Route-level RBAC matching the role table in PRODUCT_SPEC.md §3. This is a UX guard
 * only - it hides a screen a role shouldn't see, it does not substitute for server-side
 * authorization. The backend does not yet check roles on write endpoints (approve/reject,
 * submit/receive, etc.) - any authenticated user can currently call them directly; that
 * enforcement still needs to be added service-side.
 */
export function roleGuard(allowedRoles: string[]): CanActivateFn {
  // platform-admin is always allowed, matching PRODUCT_SPEC.md §3's intent that an admin
  // isn't boxed out of day-to-day screens just because Settings/Audit are their primary ones.
  const roles = [...allowedRoles, 'platform-admin'];
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (roles.some((role) => auth.hasRole(role))) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}
