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
 * Tenant and roles come from the decoded access token's claims (tenant_id, realm_access.roles)
 * - this is purely for UI display/routing; the Gateway independently re-derives tenant from
 * its own validated copy of the token, so a tampered client-side claim can't reach a backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<AuthUser | null>(null);
  readonly ready = signal(false);

  constructor(private readonly oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();
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
    const claims = this.oauthService.getIdentityClaims() as Record<string, unknown> | null;
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
}
