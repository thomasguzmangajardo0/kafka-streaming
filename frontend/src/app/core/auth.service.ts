import { Injectable, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { environment } from '../../environments/environment';

/** Fachada sobre el flujo OAuth 2.0 / OpenID Connect (authorization code + PKCE) contra Amazon Cognito. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oidc = inject(OidcSecurityService);
  private readonly router = inject(Router);

  readonly autenticado = computed(() => this.oidc.authenticated().isAuthenticated);
  readonly usuario = computed(() => {
    const datos = this.oidc.userData().userData as { email?: string; sub?: string; 'cognito:username'?: string } | null;
    return datos ?? null;
  });
  readonly email = computed(() => this.usuario()?.email ?? '');
  readonly clienteId = computed(() => this.usuario()?.['cognito:username'] ?? this.usuario()?.sub ?? '');

  /** Redirige al Hosted UI de Cognito para iniciar sesión. */
  login(): void {
    this.oidc.authorize();
  }

  /** Cierra la sesión local y la sesión en Cognito. */
  logout(): void {
    this.oidc.logoffLocal();
    const dominio = environment.oidc.cognitoDomain;
    if (dominio && !dominio.includes('<')) {
      const url =
        `${dominio}/logout?client_id=${encodeURIComponent(environment.oidc.clientId)}` +
        `&logout_uri=${encodeURIComponent(environment.oidc.postLogoutRedirectUri)}`;
      window.location.href = url;
    } else {
      void this.router.navigate(['/login']);
    }
  }
}
