package cl.duoc.streaming.pedido.controller;

import cl.duoc.streaming.pedido.config.SecurityConfig;
import cl.duoc.streaming.pedido.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el filtro JWT: sin token o con token inválido => 401; con token válido se atiende la petición. */
@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class SeguridadJwtTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    PedidoService service;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("firma inválida"));
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer token.falso.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoPermiteLaPeticion() throws Exception {
        Jwt jwt = new Jwt("tok", Instant.now(), Instant.now().plusSeconds(300), Map.of("alg", "none"), Map.of("sub", "usuario-1"));
        when(jwtDecoder.decode("token.valido.jwt")).thenReturn(jwt);
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer token.valido.jwt"))
                .andExpect(status().isOk());
    }
}
