package cl.duoc.streaming.notificacion.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
    }

    @Test
    void audienciaAceptaClaimAudDelIdToken() {
        var validador = SecurityConfig.validadorAudiencia("app-client");
        OAuth2TokenValidatorResult r = validador.validate(jwt(Map.of("sub", "u", "aud", java.util.List.of("app-client"))));
        assertFalse(r.hasErrors());
    }

    @Test
    void audienciaAceptaClientIdDelAccessTokenDeCognito() {
        var validador = SecurityConfig.validadorAudiencia("app-client");
        assertFalse(validador.validate(jwt(Map.of("sub", "u", "client_id", "app-client"))).hasErrors());
    }

    @Test
    void audienciaRechazaTokenDeOtraAplicacion() {
        var validador = SecurityConfig.validadorAudiencia("app-client");
        assertTrue(validador.validate(jwt(Map.of("sub", "u", "client_id", "otra-app"))).hasErrors());
    }

    @Test
    void sinConfiguracionDeIssuerLaValidacionFallaCerrada() {
        JwtDecoder decoder = new SecurityConfig().jwtDecoder(new SecurityProperties("", "", "", java.util.List.of()));
        assertThrows(JwtException.class, () -> decoder.decode("cualquier.token.jwt"));
    }
}
