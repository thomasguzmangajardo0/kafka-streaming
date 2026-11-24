/**
 * Configuración de DESARROLLO (ng serve en http://localhost:4200).
 * Reemplace los valores <...> con los de su Amazon Cognito y su API Gateway (ver docs/DESPLIEGUE_AWS.md).
 */
export const environment = {
  production: false,

  /** URL base del API Manager (Amazon API Gateway). En local puede apuntar directo a los microservicios. */
  apiBaseUrl: 'https://5s61sob186.execute-api.us-east-1.amazonaws.com',

  oidc: {
    /** Issuer del user pool: https://cognito-idp.<región>.amazonaws.com/<userPoolId> */
    authority: 'https://cognito-idp.us-east-1.amazonaws.com/us-east-1_CCiOJPsum',
    /** App client del user pool (público, sin secreto, con flujo "authorization code" + PKCE) */
    clientId: '306s07jon6r3gn1ll3db65emi4',
    scope: 'openid email profile',
    /** Debe estar registrado como "Allowed callback URL" en el app client */
    redirectUrl: 'http://localhost:4200',
    postLogoutRedirectUri: 'http://localhost:4200',
    /** Dominio del Hosted UI de Cognito (para cerrar sesión también en Cognito). Vacío = solo cierre local. */
    cognitoDomain: 'https://us-east-1cciojpsum.auth.us-east-1.amazoncognito.com',
  },
};
