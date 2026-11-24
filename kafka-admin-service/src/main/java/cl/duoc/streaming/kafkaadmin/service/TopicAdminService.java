package cl.duoc.streaming.kafkaadmin.service;

import cl.duoc.streaming.kafkaadmin.dto.TopicDetalle;
import cl.duoc.streaming.kafkaadmin.dto.TopicRequest;
import cl.duoc.streaming.kafkaadmin.dto.TopicResumen;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static cl.duoc.streaming.kafkaadmin.exception.KafkaFutures.await;

/** Crear, listar, describir, modificar y eliminar tópicos y sus configuraciones. */
@Service
public class TopicAdminService {

    private static final Logger log = LoggerFactory.getLogger(TopicAdminService.class);

    /** Configuraciones que este servicio permite modificar (evita cambios peligrosos accidentales). */
    static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            TopicConfig.RETENTION_MS_CONFIG,
            TopicConfig.RETENTION_BYTES_CONFIG,
            TopicConfig.CLEANUP_POLICY_CONFIG,
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
            TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
            TopicConfig.SEGMENT_MS_CONFIG,
            TopicConfig.SEGMENT_BYTES_CONFIG,
            TopicConfig.COMPRESSION_TYPE_CONFIG,
            TopicConfig.DELETE_RETENTION_MS_CONFIG);

    private final AdminClient admin;

    public TopicAdminService(AdminClient admin) {
        this.admin = admin;
    }

    public void crear(TopicRequest request) {
        Map<String, String> configs = new LinkedHashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(request.retentionMsOrDefault()));
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, request.cleanupPolicyOrDefault());
        if (request.minInsyncReplicas() != null) {
            configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(request.minInsyncReplicas()));
        }
        NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicas()).configs(configs);
        await(admin.createTopics(List.of(topic)).all());
        log.info("Tópico creado: {} (particiones={}, réplicas={}, configs={})",
                request.name(), request.partitions(), request.replicas(), configs);
    }

    public List<TopicResumen> listar() {
        Set<String> nombres = await(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = await(admin.describeTopics(nombres).allTopicNames());
        return descripciones.values().stream()
                .map(d -> new TopicResumen(d.name(), d.partitions().size(),
                        d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size()))
                .sorted(Comparator.comparing(TopicResumen::name))
                .toList();
    }

    public TopicDetalle describir(String nombre) {
        TopicDescription d = await(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = await(admin.describeConfigs(List.of(recurso)).all()).get(recurso);

        Map<String, String> explicitas = new TreeMap<>();
        for (ConfigEntry e : config.entries()) {
            if (e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                explicitas.put(e.name(), e.value());
            }
        }
        List<TopicDetalle.Particion> particiones = d.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new TopicDetalle.Particion(
                        p.partition(),
                        p.leader() != null ? p.leader().id() : -1,
                        ids(p.replicas()),
                        ids(p.isr())))
                .toList();
        return new TopicDetalle(d.name(), d.isInternal(), particiones, explicitas);
    }

    /** Modifica configuraciones (retención, cleanup policy, etc.) sin recrear el tópico. */
    public void actualizarConfiguracion(String nombre, Map<String, String> cambios) {
        for (String clave : cambios.keySet()) {
            if (!CONFIGS_PERMITIDAS.contains(clave)) {
                throw new IllegalArgumentException("Configuración no permitida: " + clave
                        + ". Permitidas: " + new TreeSet<>(CONFIGS_PERMITIDAS));
            }
        }
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Collection<AlterConfigOp> ops = new ArrayList<>();
        cambios.forEach((k, v) -> ops.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET)));
        await(admin.incrementalAlterConfigs(Map.of(recurso, ops)).all());
        log.info("Configuración actualizada en {}: {}", nombre, cambios);
    }

    /** Aumenta el total de particiones (Kafka no permite reducirlas). */
    public void aumentarParticiones(String nombre, int total) {
        await(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("Particiones de {} aumentadas a {}", nombre, total);
    }

    public void eliminar(String nombre) {
        if (nombre.startsWith("__")) {
            throw new IllegalArgumentException("No se pueden eliminar tópicos internos de Kafka: " + nombre);
        }
        await(admin.deleteTopics(List.of(nombre)).all());
        log.warn("Tópico eliminado: {}", nombre);
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }
}
