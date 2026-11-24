package cl.duoc.streaming.notificacion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Seguridad del microservicio como Resource Server OAuth2.
 *
 * <p>El filtro {@code BearerTokenAuthenticationFilter} extrae el JWT del header
 * {@code Authorization: Bearer ...}, valida firma (JWKS del IDaaS), expiración, issuer y
 * audiencia/client_id. Sin token válido la respuesta es 401 y la petición nunca llega al controlador.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sin sesión ni cookies: CSRF no aplica
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** Decodificador creado de forma perezosa: el servicio arranca aunque el IDaaS no responda aún. */
    @Bean
    public JwtDecoder jwtDecoder(SecurityProperties props) {
        return new LazyJwtDecoder(() -> construirDecoder(props));
    }

    static JwtDecoder construirDecoder(SecurityProperties props) {
        boolean hayJwks = StringUtils.hasText(props.jwkSetUri());
        boolean hayIssuer = StringUtils.hasText(props.issuerUri());
        if (!hayJwks && !hayIssuer) {
            throw new JwtException("Seguridad sin configurar: defina IDAAS_ISSUER_URI (o IDAAS_JWK_SET_URI)");
        }
        // Cognito y Auth0 publican el JWKS en <issuer>/.well-known/jwks.json
        String jwks = hayJwks ? props.jwkSetUri()
                : props.issuerUri().replaceAll("/+$", "") + "/.well-known/jwks.json";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();

        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>();
        validadores.add(hayIssuer ? JwtValidators.createDefaultWithIssuer(props.issuerUri())
                : JwtValidators.createDefault());
        if (StringUtils.hasText(props.audience())) {
            validadores.add(validadorAudiencia(props.audience()));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadores));
        log.info("Validación JWT activa (issuer={}, audience={})", props.issuerUri(), props.audience());
        return decoder;
    }

    /** Cognito entrega {@code client_id} en el access token y {@code aud} en el id token: se acepta cualquiera. */
    static OAuth2TokenValidator<Jwt> validadorAudiencia(String esperada) {
        return jwt -> {
            List<String> aud = jwt.getAudience();
            String clientId = jwt.getClaimAsString("client_id");
            if ((aud != null && aud.contains(esperada)) || esperada.equals(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El token no fue emitido para esta aplicación", null));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.corsAllowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /** Crea el decodificador real en la primera petición protegida. */
    static final class LazyJwtDecoder implements JwtDecoder {
        private final Supplier<JwtDecoder> fabrica;
        private volatile JwtDecoder delegado;

        LazyJwtDecoder(Supplier<JwtDecoder> fabrica) {
            this.fabrica = fabrica;
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            JwtDecoder d = delegado;
            if (d == null) {
                synchronized (this) {
                    if (delegado == null) {
                        try {
                            delegado = fabrica.get();
                        } catch (JwtException e) {
                            throw e;
                        } catch (RuntimeException e) {
                            throw new JwtException("No se pudo contactar al IDaaS: " + e.getMessage(), e);
                        }
                    }
                    d = delegado;
                }
            }
            return d.decode(token);
        }
    }
}
