package cl.duoc.streaming.pedido.controller;

import cl.duoc.streaming.pedido.domain.EstadoPedido;
import cl.duoc.streaming.pedido.domain.Pedido;
import cl.duoc.streaming.pedido.exception.PedidoNoEncontradoException;
import cl.duoc.streaming.pedido.service.PedidoService;
import cl.duoc.streaming.pedido.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
@WithMockUser // autenticación simulada: la validación real del JWT se prueba en SeguridadJwtTest
class PedidoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    PedidoService service;

    private Pedido pedido() {
        Instant now = Instant.now();
        return new Pedido("p-1", "cli-1", "cli@duoc.cl", List.of(), new BigDecimal("3000"),
                EstadoPedido.PENDIENTE, null, now, now);
    }

    @Test
    void postValidoDevuelve202() throws Exception {
        when(service.crear(any())).thenReturn(pedido());
        String body = """
                {"clienteId":"cli-1","clienteEmail":"cli@duoc.cl",
                 "items":[{"sku":"SKU-001","cantidad":2,"precioUnitario":1500}]}""";
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("p-1"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void postInvalidoDevuelve400() throws Exception {
        String body = """
                {"clienteId":"","clienteEmail":"no-es-email","items":[]}""";
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInexistenteDevuelve404() throws Exception {
        when(service.obtener("x")).thenThrow(new PedidoNoEncontradoException("x"));
        mvc.perform(get("/api/pedidos/x")).andExpect(status().isNotFound());
    }
}
