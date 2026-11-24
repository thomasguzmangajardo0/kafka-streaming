# Frontend Angular · Pedidos en streaming

Angular 22 (componentes standalone + signals). Inicia sesión con **OAuth 2.0 / OpenID Connect** contra
**Amazon Cognito** (flujo *authorization code + PKCE*) y llama al backend **a través del API Manager**
(Amazon API Gateway) enviando el JWT en `Authorization: Bearer`.

## Configuración

Edite los valores `<...>` de:

- `src/environments/environment.ts` (desarrollo, `ng serve`)
- `src/environments/environment.prod.ts` (producción, `ng build`)

| Campo | Qué es |
|---|---|
| `apiBaseUrl` | URL del API Gateway (o de los microservicios en local) |
| `oidc.authority` | `https://cognito-idp.<región>.amazonaws.com/<userPoolId>` |
| `oidc.clientId` | App client del user pool (sin secreto) |
| `oidc.redirectUrl` / `postLogoutRedirectUri` | URL del frontend; deben estar registradas en el app client |
| `oidc.cognitoDomain` | Dominio del Hosted UI, para cerrar también la sesión en Cognito |

## Comandos

```bash
npm install
npm start          # http://localhost:4200
npm run build      # dist/frontend/browser  (subir a S3 / CloudFront o servir con nginx)
```

## Estructura

```
src/app/core/    auth.service (login/logout), auth.guard, error.interceptor, api.service, polling, models
src/app/pages/   login, pedidos (crear + estado en vivo), inventario (stock + despachos), notificaciones (+ auditoría)
```

El interceptor `authInterceptor()` de `angular-auth-oidc-client` solo agrega el token a las URLs de
`secureRoutes` (la URL del API Manager). Ante un `401` la app vuelve a iniciar sesión.
