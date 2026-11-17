import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

/**
 * Attaches the access token so the Gateway can validate it and derive tenant from the
 * token's tenant_id claim (README §2) - this replaces the old tenant.interceptor.ts,
 * which just sent a client-chosen X-Tenant-Id header the backend trusted outright.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
