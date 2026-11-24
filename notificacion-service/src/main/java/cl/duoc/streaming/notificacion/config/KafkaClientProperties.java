package cl.duoc.streaming.notificacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades del cliente Kafka (prefijo {@code app.kafka}). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaClientProperties(
        String bootstrapServers,
        Topics topics,
        Producer producer,
        Consumer consumer) {

    public record Topics(String pedidos, String inventario) {
    }

    public record Producer(
            String acks,
            int retries,
            int lingerMs,
            int batchSize,
            String compressionType,
            int requestTimeoutMs,
            int deliveryTimeoutMs) {
    }

    public record Consumer(
            String pedidosGroupId,
            String stockGroupId,
            String autoOffsetReset,
            int maxPollRecords,
            int maxPollIntervalMs,
            int sessionTimeoutMs,
            int heartbeatIntervalMs,
            int concurrency,
            int retryMaxAttempts,
            long retryBackoffMs) {
    }
}
