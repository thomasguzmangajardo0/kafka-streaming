package cl.duoc.streaming.inventario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Parámetros de seguridad (prefijo {@code app.security}). Se alimentan por variables de entorno
 * para no dejar datos del IDaaS (Amazon Cognito u otro proveedor OIDC) dentro del código.
 *
 * @param issuerUri          issuer del token, p.ej. https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXX
 * @param jwkSetUri          (opcional) URL del JWKS; si está vacío se usa <issuer>/.well-known/jwks.json
 * @param audience           (opcional) app client id esperado; se valida contra el claim aud o client_id
 * @param corsAllowedOrigins orígenes permitidos para el frontend
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @DefaultValue("") String issuerUri,
        @DefaultValue("") String jwkSetUri,
        @DefaultValue("") String audience,
        @DefaultValue("http://localhost:4200") List<String> corsAllowedOrigins) {
}
