package cl.duoc.streaming.inventario.config;

import cl.duoc.streaming.inventario.dto.PedidoEvento;
import cl.duoc.streaming.inventario.dto.StockEvento;
import cl.duoc.streaming.inventario.exception.PayloadInvalidoException;
import cl.duoc.streaming.inventario.messaging.DeadLetterLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores: reintentos con backoff exponencial y, al agotarse (o si el error no es
 * reintentable), envío a {@code pedidos.eventos.DLT} + log. Un mensaje defectuoso nunca bloquea la partición.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean("dltKafkaTemplate")
    public KafkaTemplate<String, Object> dltKafkaTemplate(KafkaClientProperties props, ObjectMapper mapper) {
        JsonSerializer<Object> json = new JsonSerializer<>(mapper);
        json.setAddTypeInfo(false);
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(PedidoEvento.class, json);
        delegates.put(StockEvento.class, json);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaProducerConfig.baseProducerConfig(props),
                new StringSerializer(),
                new DelegatingByTypeSerializer(delegates)));
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("dltKafkaTemplate") KafkaTemplate<String, Object> dltKafkaTemplate,
            KafkaClientProperties props) {
        DeadLetterPublishingRecoverer publisher = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(props.consumer().retryMaxAttempts());
        backOff.setInitialInterval(props.consumer().retryBackoffMs());
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterLogger(publisher), backOff);
        handler.addNotRetryableExceptions(PayloadInvalidoException.class);
        return handler;
    }
}
