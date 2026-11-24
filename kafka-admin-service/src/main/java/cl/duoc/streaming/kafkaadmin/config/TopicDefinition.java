package cl.duoc.streaming.kafkaadmin.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/** Definición declarativa de un tópico (particiones, réplicas, retención, cleanup policy). */
public record TopicDefinition(
        String name,
        int partitions,
        short replicas,
        long retentionMs,
        String cleanupPolicy,
        int minInsyncReplicas) {

    public NewTopic toNewTopic() {
        Map<String, String> configs = new LinkedHashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
        configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(minInsyncReplicas));
        return new NewTopic(name, partitions, replicas).configs(configs);
    }
}
