package cl.duoc.streaming.inventario.config;

import cl.duoc.streaming.inventario.dto.StockEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/** Productor de {@link StockEvento}: clave String, valor JSON, acks=all + idempotencia. */
@Configuration
@EnableConfigurationProperties({KafkaClientProperties.class, InventarioProperties.class})
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

    @Bean
    public ProducerFactory<String, StockEvento> stockProducerFactory(KafkaClientProperties props, ObjectMapper mapper) {
        JsonSerializer<StockEvento> valueSerializer = new JsonSerializer<>(mapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(baseProducerConfig(props), new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, StockEvento> stockKafkaTemplate(ProducerFactory<String, StockEvento> stockProducerFactory) {
        return new KafkaTemplate<>(stockProducerFactory);
    }
}
