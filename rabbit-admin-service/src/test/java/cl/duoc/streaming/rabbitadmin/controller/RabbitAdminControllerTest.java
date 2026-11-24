package cl.duoc.streaming.rabbitadmin.controller;

import cl.duoc.streaming.rabbitadmin.config.SecurityConfig;
import cl.duoc.streaming.rabbitadmin.service.RabbitManagementClient;
import cl.duoc.streaming.rabbitadmin.service.TopologiaInicial;
import cl.duoc.streaming.rabbitadmin.service.TopologiaService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
@Import(SecurityConfig.class)
class RabbitAdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    TopologiaService topologia;

    @MockitoBean
    RabbitManagementClient management;

    @MockitoBean
    TopologiaInicial topologiaInicial;

    private static final String BEARER = "Bearer token.valido.jwt";

    private void tokenValido() {
        Jwt jwt = new Jwt("tok", Instant.now(), Instant.now().plusSeconds(300), Map.of("alg", "none"), Map.of("sub", "admin-1"));
        when(jwtDecoder.decode("token.valido.jwt")).thenReturn(jwt);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/rabbit/queues")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("firma inválida"));
        mvc.perform(get("/api/rabbit/queues").header("Authorization", "Bearer falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clusterMuestraLosDosNodos() throws Exception {
        tokenValido();
        when(management.cluster()).thenReturn(Map.of("totalNodos", 2, "enCluster", true));
        mvc.perform(get("/api/rabbit/cluster").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodos").value(2))
                .andExpect(jsonPath("$.enCluster").value(true));
    }

    @Test
    void listarColas() throws Exception {
        tokenValido();
        when(management.colas()).thenReturn(List.of(Map.of("name", "cola.auditoria", "consumers", 1)));
        mvc.perform(get("/api/rabbit/queues").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("cola.auditoria"));
    }

    @Test
    void crearColaValidaDevuelve201() throws Exception {
        tokenValido();
        mvc.perform(post("/api/rabbit/queues").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"cola.nueva\",\"conDlq\":true}"))
                .andExpect(status().isCreated());
    }

    @Test
    void crearColaConNombreInvalidoDevuelve400() throws Exception {
        tokenValido();
        mvc.perform(post("/api/rabbit/queues").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nombre con espacios\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearExchangeConTipoInvalidoDevuelve400() throws Exception {
        tokenValido();
        mvc.perform(post("/api/rabbit/exchanges").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ex.x\",\"type\":\"raro\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarColaInexistenteDevuelve404() throws Exception {
        tokenValido();
        when(topologia.eliminarCola("no-existe")).thenReturn(false);
        mvc.perform(delete("/api/rabbit/queues/no-existe").header("Authorization", BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarColaExistenteDevuelve204() throws Exception {
        tokenValido();
        when(topologia.eliminarCola("cola.vieja")).thenReturn(true);
        mvc.perform(delete("/api/rabbit/queues/cola.vieja").header("Authorization", BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    void recursoProtegidoDevuelve400() throws Exception {
        tokenValido();
        when(topologia.eliminarExchange("amq.direct")).thenThrow(new IllegalArgumentException("protegido"));
        mvc.perform(delete("/api/rabbit/exchanges/amq.direct").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brokerCaidoDevuelve503() throws Exception {
        tokenValido();
        doThrow(new AmqpConnectException(new ConnectException("refused"))).when(topologia).enlazar(any());
        mvc.perform(post("/api/rabbit/bindings").header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"eventos.topic\",\"queue\":\"cola.auditoria\",\"routingKey\":\"pedido.#\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
