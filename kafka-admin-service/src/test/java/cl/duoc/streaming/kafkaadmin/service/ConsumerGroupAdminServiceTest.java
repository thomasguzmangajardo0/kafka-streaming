package cl.duoc.streaming.kafkaadmin.service;

import cl.duoc.streaming.kafkaadmin.dto.LagGrupo;
import cl.duoc.streaming.kafkaadmin.exception.RecursoNoEncontradoException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerGroupAdminServiceTest {

    private AdminClient admin;
    private ConsumerGroupAdminService service;

    @BeforeEach
    void setUp() {
        admin = mock(AdminClient.class);
        service = new ConsumerGroupAdminService(admin);
    }

    private void grupoConEstado(String groupId, ConsumerGroupState estado) {
        ConsumerGroupDescription d = mock(ConsumerGroupDescription.class);
        when(d.state()).thenReturn(estado);
        DescribeConsumerGroupsResult r = mock(DescribeConsumerGroupsResult.class);
        when(r.all()).thenReturn(KafkaFuture.completedFuture(Map.of(groupId, d)));
        when(admin.describeConsumerGroups(any(Collection.class))).thenReturn(r);
    }

    @Test
    void lagEsLaDiferenciaEntreElFinalDelLogYElOffsetConfirmado() {
        grupoConEstado("inventario-group", ConsumerGroupState.STABLE);

        TopicPartition p0 = new TopicPartition("pedidos.eventos", 0);
        TopicPartition p1 = new TopicPartition("pedidos.eventos", 1);
        ListConsumerGroupOffsetsResult confirmados = mock(ListConsumerGroupOffsetsResult.class);
        when(confirmados.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(
                Map.of(p0, new OffsetAndMetadata(10), p1, new OffsetAndMetadata(4))));
        when(admin.listConsumerGroupOffsets(anyString())).thenReturn(confirmados);

        ListOffsetsResult finales = mock(ListOffsetsResult.class);
        when(finales.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                p0, new ListOffsetsResultInfo(15, 0L, Optional.empty()),
                p1, new ListOffsetsResultInfo(4, 0L, Optional.empty()))));
        when(admin.listOffsets(any(Map.class))).thenReturn(finales);

        LagGrupo lag = service.lag("inventario-group");

        assertEquals(5, lag.lagTotal());
        assertEquals(2, lag.partitions().size());
        assertEquals(5, lag.partitions().get(0).lag()); // partición 0: 15 - 10
        assertEquals(0, lag.partitions().get(1).lag()); // partición 1 al día
    }

    @Test
    void grupoInexistenteDevuelveNoEncontrado() {
        grupoConEstado("fantasma", ConsumerGroupState.DEAD);
        assertThrows(RecursoNoEncontradoException.class, () -> service.lag("fantasma"));
    }

    @Test
    void sinOffsetsConfirmadosElLagEsCero() {
        grupoConEstado("nuevo", ConsumerGroupState.EMPTY);
        ListConsumerGroupOffsetsResult vacio = mock(ListConsumerGroupOffsetsResult.class);
        when(vacio.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        when(admin.listConsumerGroupOffsets(anyString())).thenReturn(vacio);

        LagGrupo lag = service.lag("nuevo");

        assertEquals(0, lag.lagTotal());
        assertEquals(List.of(), lag.partitions());
    }
}
