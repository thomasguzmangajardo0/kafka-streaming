package cl.duoc.streaming.kafkaadmin.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicDefinitionTest {

    @Test
    void convierteParticionesReplicasYConfiguracionesDeAdministracion() {
        TopicDefinition def = new TopicDefinition("inventario.eventos", 3, (short) 3, 2_592_000_000L, "compact,delete", 2);

        NewTopic topic = def.toNewTopic();

        assertEquals("inventario.eventos", topic.name());
        assertEquals(3, topic.numPartitions());
        assertEquals((short) 3, topic.replicationFactor());
        assertEquals("2592000000", topic.configs().get("retention.ms"));
        assertEquals("compact,delete", topic.configs().get("cleanup.policy"));
        assertEquals("2", topic.configs().get("min.insync.replicas"));
    }
}
