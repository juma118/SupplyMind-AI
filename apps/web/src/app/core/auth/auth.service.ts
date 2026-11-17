import { Injectable, signal } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  name: string;
  username: string;
  tenantId: string;
  roles: string[];
}

const authConfig: AuthConfig = {
  issuer: environment.keycloak.issuer,
  redirectUri: window.location.origin,
  clientId: environment.keycloak.clientId,
  responseType: 'code',
  scope: 'openid profile email',
  showDebugInformation: !environment.production,
  requireHttps: false,
};

/**
 * Wraps angular-oauth2-oidc so the rest of the app never touches OAuthService directly.
 * Tenant and roles come from the decoded ACCESS token's claims (tenant_id, realm_access.roles)
 * - this is purely for UI display/routing; the Gateway independently re-derives tenant from
 * its own validated copy of the token, so a tampered client-side claim can't reach a backend.
 *
 * Deliberately not getIdentityClaims() (the ID token): Keycloak's default realm-roles mapper
 * only puts realm_access.roles on the access token, not the ID token, so reading roles from
 * the ID token silently comes back empty for every user.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<AuthUser | null>(null);
  readonly ready = signal(false);

  constructor(private readonly oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();
    this.oauthService.events.subscribe((event) => {
      if (event.type === 'token_received' || event.type === 'silently_refreshed') {
        this.refreshUser();
      }
    });
  }

  async initialize(): Promise<void> {
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();
    this.refreshUser();
    this.ready.set(true);
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  isLoggedIn(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  getAccessToken(): string | null {
    return this.oauthService.getAccessToken() || null;
  }

  hasRole(role: string): boolean {
    return this.user()?.roles.includes(role) ?? false;
  }

  private refreshUser(): void {
    if (!this.isLoggedIn()) {
      this.user.set(null);
      return;
    }
    const claims = this.decodeAccessToken();
    if (!claims) {
      this.user.set(null);
      return;
    }
    const realmAccess = claims['realm_access'] as { roles?: string[] } | undefined;
    this.user.set({
      name: (claims['name'] as string) ?? (claims['preferred_username'] as string) ?? 'Unknown',
      username: (claims['preferred_username'] as string) ?? '',
      tenantId: (claims['tenant_id'] as string) ?? '',
      roles: realmAccess?.roles ?? [],
    });
  }

  private decodeAccessToken(): Record<string, unknown> | null {
    const token = this.oauthService.getAccessToken();
    if (!token) {
      return null;
    }
    const payload = token.split('.')[1];
    if (!payload) {
      return null;
    }
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    try {
      return JSON.parse(decodeURIComponent(escape(atob(base64))));
    } catch {
      return null;
    }
  }
}
