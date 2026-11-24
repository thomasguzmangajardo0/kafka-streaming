package cl.duoc.streaming.kafkaadmin.service;

import cl.duoc.streaming.kafkaadmin.dto.GrupoDetalle;
import cl.duoc.streaming.kafkaadmin.dto.GrupoResumen;
import cl.duoc.streaming.kafkaadmin.dto.LagGrupo;
import cl.duoc.streaming.kafkaadmin.exception.RecursoNoEncontradoException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cl.duoc.streaming.kafkaadmin.exception.KafkaFutures.await;

/** Inspección y mantenimiento de consumer groups: estado, miembros, lag, reinicio de offsets y borrado. */
@Service
public class ConsumerGroupAdminService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerGroupAdminService.class);

    private final AdminClient admin;

    public ConsumerGroupAdminService(AdminClient admin) {
        this.admin = admin;
    }

    public List<GrupoResumen> listar() {
        return await(admin.listConsumerGroups().all()).stream()
                .map(g -> new GrupoResumen(g.groupId(), estado(g)))
                .sorted(Comparator.comparing(GrupoResumen::groupId))
                .toList();
    }

    public GrupoDetalle describir(String groupId) {
        ConsumerGroupDescription d = descripcion(groupId);
        List<GrupoDetalle.Miembro> miembros = d.members().stream()
                .map(m -> new GrupoDetalle.Miembro(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .map(tp -> tp.topic() + "-" + tp.partition())
                                .sorted()
                                .toList()))
                .toList();
        String coordinador = d.coordinator() != null ? d.coordinator().host() + ":" + d.coordinator().port() : "n/d";
        return new GrupoDetalle(d.groupId(), String.valueOf(d.state()), d.partitionAssignor(), coordinador, miembros);
    }

    /** Lag por partición = último offset del log - offset confirmado por el grupo. */
    public LagGrupo lag(String groupId) {
        descripcion(groupId); // 404 si el grupo no existe
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                await(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        if (confirmados.isEmpty()) {
            return new LagGrupo(groupId, 0, List.of());
        }
        Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
        confirmados.keySet().forEach(tp -> latest.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> finales = await(admin.listOffsets(latest).all());

        List<LagGrupo.LagParticion> particiones = confirmados.entrySet().stream()
                .map(e -> {
                    long actual = e.getValue().offset();
                    long fin = finales.get(e.getKey()).offset();
                    return new LagGrupo.LagParticion(e.getKey().topic(), e.getKey().partition(),
                            actual, fin, Math.max(0, fin - actual));
                })
                .sorted(Comparator.comparing(LagGrupo.LagParticion::topic)
                        .thenComparingInt(LagGrupo.LagParticion::partition))
                .toList();
        long total = particiones.stream().mapToLong(LagGrupo.LagParticion::lag).sum();
        return new LagGrupo(groupId, total, particiones);
    }

    /** Reubica los offsets de un grupo inactivo (sin consumidores conectados) al inicio o al final del tópico. */
    public void reiniciarOffsets(String groupId, String topic, String destino) {
        ConsumerGroupDescription grupo = descripcion(groupId);
        if (!grupo.members().isEmpty()) {
            throw new IllegalArgumentException("El grupo " + groupId
                    + " tiene consumidores activos; detén los servicios antes de reiniciar offsets");
        }
        TopicDescription topico = await(admin.describeTopics(List.of(topic)).allTopicNames()).get(topic);
        OffsetSpec spec = "earliest".equals(destino) ? OffsetSpec.earliest() : OffsetSpec.latest();

        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        topico.partitions().forEach(p -> consulta.put(new TopicPartition(topic, p.partition()), spec));
        Map<TopicPartition, ListOffsetsResultInfo> offsets = await(admin.listOffsets(consulta).all());

        Map<TopicPartition, OffsetAndMetadata> nuevos = new HashMap<>();
        offsets.forEach((tp, info) -> nuevos.put(tp, new OffsetAndMetadata(info.offset())));
        await(admin.alterConsumerGroupOffsets(groupId, nuevos).all());
        log.info("Offsets del grupo {} en {} reubicados a {}", groupId, topic, destino);
    }

    /** Elimina un grupo (solo posible si no tiene miembros activos). */
    public void eliminar(String groupId) {
        descripcion(groupId);
        await(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.warn("Consumer group eliminado: {}", groupId);
    }

    private ConsumerGroupDescription descripcion(String groupId) {
        ConsumerGroupDescription d = await(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        if (d == null || d.state() == ConsumerGroupState.DEAD) {
            throw new RecursoNoEncontradoException("Consumer group no encontrado: " + groupId);
        }
        return d;
    }

    private static String estado(ConsumerGroupListing g) {
        return g.state().map(Object::toString).orElse("UNKNOWN");
    }
}
