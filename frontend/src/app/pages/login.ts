import { Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  template: `
    <section class="card login">
      <h1>Inicia sesión</h1>
      <p class="sub">
        La autenticación la gestiona Amazon Cognito (OAuth 2.0 / OpenID Connect). Al ingresar recibirás un JWT que se
        envía en cada llamada al API.
      </p>
      <button type="button" (click)="auth.login()">Ingresar con Cognito</button>
    </section>
  `,
  styles: `
    .login { max-width: 460px; margin: 8vh auto 0; }
    .login button { width: 100%; margin-top: 4px; }
  `,
})
export class Login {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    // Si ya hay sesión, no tiene sentido mostrar esta pantalla
    effect(() => {
      if (this.auth.autenticado()) {
        void this.router.navigate(['/pedidos']);
      }
    });
  }
}
