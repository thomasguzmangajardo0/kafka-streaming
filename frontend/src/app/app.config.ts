import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  inject,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { authInterceptor, LogLevel, OidcSecurityService, provideAuth } from 'angular-auth-oidc-client';
import { catchError, firstValueFrom, of } from 'rxjs';
import { environment } from '../environments/environment';
import { errorInterceptor } from './core/error.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor(), errorInterceptor])),

    // Login con IDaaS (Amazon Cognito) mediante OAuth 2.0 / OpenID Connect: código de autorización + PKCE
    provideAuth({
      config: {
        authority: environment.oidc.authority,
        redirectUrl: environment.oidc.redirectUrl,
        postLogoutRedirectUri: environment.oidc.postLogoutRedirectUri,
        clientId: environment.oidc.clientId,
        scope: environment.oidc.scope,
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        renewTimeBeforeTokenExpiresInSeconds: 30,
        // El JWT solo se adjunta a las llamadas hacia el API Manager (nunca a terceros)
        secureRoutes: [environment.apiBaseUrl],
        logLevel: environment.production ? LogLevel.Error : LogLevel.Warn,
      },
    }),

    // Al arrancar procesa el retorno del Hosted UI (?code=...) y restaura la sesión si ya existe.
    // Si el IDaaS no responde, la app igual carga y muestra la pantalla de login.
    provideAppInitializer(() =>
      firstValueFrom(inject(OidcSecurityService).checkAuth().pipe(catchError(() => of(null)))),
    ),
  ],
};
