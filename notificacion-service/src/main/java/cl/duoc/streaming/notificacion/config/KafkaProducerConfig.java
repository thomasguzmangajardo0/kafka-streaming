package cl.duoc.streaming.notificacion.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Este servicio no publica eventos de negocio; solo necesita propiedades de productor
 * para escribir en las Dead Letter Topics (ver {@link KafkaErrorHandlingConfig}).
 */
@Configuration
@EnableConfigurationProperties(KafkaClientProperties.class)
public class KafkaProducerConfig {

    static Map<String, Object> baseProducerConfig(KafkaClientProperties props) {
        KafkaClientProperties.Producer p = props.producer();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        cfg.put(ProducerConfig.ACKS_CONFIG, p.acks());
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        cfg.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        cfg.put(ProducerConfig.RETRIES_CONFIG, p.retries());
        cfg.put(ProducerConfig.LINGER_MS_CONFIG, p.lingerMs());
        cfg.put(ProducerConfig.BATCH_SIZE_CONFIG, p.batchSize());
        cfg.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, p.compressionType());
        cfg.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, p.requestTimeoutMs());
        cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, p.deliveryTimeoutMs());
        return cfg;
    }
}
