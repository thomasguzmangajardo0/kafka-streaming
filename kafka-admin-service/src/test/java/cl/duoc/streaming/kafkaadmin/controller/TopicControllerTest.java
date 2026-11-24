package cl.duoc.streaming.kafkaadmin.controller;

import cl.duoc.streaming.kafkaadmin.dto.TopicResumen;
import cl.duoc.streaming.kafkaadmin.exception.KafkaOperacionException;
import cl.duoc.streaming.kafkaadmin.service.TopicAdminService;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import cl.duoc.streaming.kafkaadmin.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicController.class)
@Import(SecurityConfig.class)
@WithMockUser // autenticación simulada: la validación real del JWT se prueba en SeguridadJwtTest
class TopicControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    TopicAdminService service;

    @Test
    void listarDevuelveTopicos() throws Exception {
        when(service.listar()).thenReturn(List.of(new TopicResumen("pedidos.eventos", 3, 3)));
        mvc.perform(get("/api/kafka/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("pedidos.eventos"))
                .andExpect(jsonPath("$[0].partitions").value(3))
                .andExpect(jsonPath("$[0].replicationFactor").value(3));
    }

    @Test
    void crearValidoDevuelve201() throws Exception {
        String body = """
                {"name":"nuevo.topico","partitions":3,"replicas":3,"retentionMs":86400000,
                 "cleanupPolicy":"delete","minInsyncReplicas":2}""";
        mvc.perform(post("/api/kafka/topics").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void crearConParticionesInvalidasDevuelve400() throws Exception {
        String body = """
                {"name":"nuevo.topico","partitions":0,"replicas":3}""";
        mvc.perform(post("/api/kafka/topics").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearTopicoExistenteDevuelve409() throws Exception {
        doThrow(new KafkaOperacionException("ya existe", new TopicExistsException("ya existe")))
                .when(service).crear(any());
        String body = """
                {"name":"pedidos.eventos","partitions":3,"replicas":3}""";
        mvc.perform(post("/api/kafka/topics").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void describirTopicoInexistenteDevuelve404() throws Exception {
        when(service.describir("x")).thenThrow(
                new KafkaOperacionException("no existe", new UnknownTopicOrPartitionException("no existe")));
        mvc.perform(get("/api/kafka/topics/x")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete("/api/kafka/topics/tmp")).andExpect(status().isNoContent());
    }

    @Test
    void eliminarTopicoInternoDevuelve400() throws Exception {
        doThrow(new IllegalArgumentException("interno")).when(service).eliminar("__consumer_offsets");
        mvc.perform(delete("/api/kafka/topics/__consumer_offsets")).andExpect(status().isBadRequest());
    }
}
