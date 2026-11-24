package cl.duoc.streaming.inventario.config;

import cl.duoc.streaming.inventario.dto.PedidoEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumidor de {@link PedidoEvento} en el consumer group {@code inventario-group}.
 * Con concurrency=3 cada instancia levanta 3 hilos; las 3 particiones se reparten entre
 * todos los hilos / instancias del grupo (balanceo de carga + resiliencia ante caídas).
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, PedidoEvento> pedidoConsumerFactory(KafkaClientProperties props, ObjectMapper mapper) {
        KafkaClientProperties.Consumer c = props.consumer();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, c.groupId());
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, c.autoOffsetReset());
        cfg.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        cfg.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, c.maxPollRecords());
        cfg.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, c.maxPollIntervalMs());
        cfg.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, c.sessionTimeoutMs());
        cfg.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, c.heartbeatIntervalMs());
        cfg.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        cfg.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        JsonDeserializer<PedidoEvento> json = new JsonDeserializer<>(PedidoEvento.class, mapper, false);
        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), new ErrorHandlingDeserializer<>(json));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PedidoEvento> pedidoKafkaListenerContainerFactory(
            ConsumerFactory<String, PedidoEvento> pedidoConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            KafkaClientProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, PedidoEvento> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(pedidoConsumerFactory);
        factory.setConcurrency(props.consumer().concurrency());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
