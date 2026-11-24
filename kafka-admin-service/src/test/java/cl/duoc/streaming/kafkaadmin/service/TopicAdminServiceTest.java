package cl.duoc.streaming.kafkaadmin.service;

import cl.duoc.streaming.kafkaadmin.dto.TopicRequest;
import cl.duoc.streaming.kafkaadmin.exception.KafkaOperacionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicAdminServiceTest {

    private AdminClient admin;
    private TopicAdminService service;

    @BeforeEach
    void setUp() {
        admin = mock(AdminClient.class);
        service = new TopicAdminService(admin);
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearEnviaParticionesReplicasYConfiguraciones() {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(any(Collection.class))).thenReturn(result);

        service.crear(new TopicRequest("pedidos.eventos", 3, (short) 3, 604_800_000L, "delete", 2));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        NewTopic enviado = captor.getValue().iterator().next();
        assertEquals("pedidos.eventos", enviado.name());
        assertEquals(3, enviado.numPartitions());
        assertEquals((short) 3, enviado.replicationFactor());
        assertEquals("604800000", enviado.configs().get("retention.ms"));
        assertEquals("delete", enviado.configs().get("cleanup.policy"));
        assertEquals("2", enviado.configs().get("min.insync.replicas"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearUsaValoresPorDefectoSiNoSeIndican() {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(any(Collection.class))).thenReturn(result);

        service.crear(new TopicRequest("nuevo", 1, (short) 1, null, null, null));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        Map<String, String> configs = captor.getValue().iterator().next().configs();
        assertEquals("604800000", configs.get("retention.ms"));
        assertEquals("delete", configs.get("cleanup.policy"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void topicoExistenteSeReportaConLaCausaOriginalDeKafka() {
        KafkaFutureImpl<Void> fallo = new KafkaFutureImpl<>();
        fallo.completeExceptionally(new TopicExistsException("ya existe"));
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(fallo);
        when(admin.createTopics(any(Collection.class))).thenReturn(result);

        KafkaOperacionException ex = assertThrows(KafkaOperacionException.class,
                () -> service.crear(new TopicRequest("pedidos.eventos", 3, (short) 3, null, null, null)));
        assertInstanceOf(TopicExistsException.class, ex.getCause());
    }

    @Test
    void noPermiteModificarConfiguracionesFueraDeLaListaBlanca() {
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarConfiguracion("pedidos.eventos", Map.of("unclean.leader.election.enable", "true")));
        verify(admin, never()).incrementalAlterConfigs(any());
    }

    @Test
    void noPermiteEliminarTopicosInternos() {
        assertThrows(IllegalArgumentException.class, () -> service.eliminar("__consumer_offsets"));
        verify(admin, never()).deleteTopics(any(Collection.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void eliminarInvocaDeleteTopics() {
        DeleteTopicsResult result = mock(DeleteTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.deleteTopics(any(Collection.class))).thenReturn(result);

        service.eliminar("tmp.topic");

        verify(admin).deleteTopics(any(Collection.class));
    }
}
