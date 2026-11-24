package cl.duoc.streaming.pedido.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del cliente Kafka (prefijo {@code app.kafka}). Centraliza en un solo lugar
 * brokers, tópicos y ajustes de productor / consumidor.
 */
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
            String groupId,
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
