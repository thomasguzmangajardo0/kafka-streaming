package cl.duoc.streaming.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.kafka")
public record AdminKafkaProperties(String bootstrapServers, List<TopicDefinition> topics) {

    public AdminKafkaProperties {
        topics = topics == null ? List.of() : List.copyOf(topics);
    }
}
