/**
 * Configuración de PRODUCCIÓN (ng build). Complete con los datos de su despliegue en AWS.
 * redirectUrl y postLogoutRedirectUri deben ser HTTPS (CloudFront) y estar registradas en el app client.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://<API_ID>.execute-api.us-east-1.amazonaws.com',
  oidc: {
    authority: 'https://cognito-idp.us-east-1.amazonaws.com/us-east-1_CCiOJPsum',
    clientId: '306s07jon6r3gn1ll3db65emi4',
    scope: 'openid email profile',
    redirectUrl: 'https://<DISTRIBUCION>.cloudfront.net',
    postLogoutRedirectUri: 'https://<DISTRIBUCION>.cloudfront.net',
    cognitoDomain: 'https://us-east-1cciojpsum.auth.us-east-1.amazoncognito.com',
  },
};
