package cl.duoc.streaming.pedido.config;

import cl.duoc.streaming.pedido.dto.StockEvento;
import cl.duoc.streaming.pedido.exception.PayloadInvalidoException;
import cl.duoc.streaming.pedido.exception.PedidoNoEncontradoException;
import cl.duoc.streaming.pedido.messaging.DeadLetterLogger;
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
 * Estrategia de errores del consumidor:
 * 1) validación / log contextual, 2) reintentos con backoff exponencial,
 * 3) si se agotan (o el error no es reintentable) el mensaje va al tópico {@code <topico>.DLT}
 *    y se deja registro en el log. El consumidor sigue con el siguiente mensaje.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    /** Template para la DLT: acepta byte[] (mensajes no deserializables) y eventos tipados. */
    @Bean("dltKafkaTemplate")
    public KafkaTemplate<String, Object> dltKafkaTemplate(KafkaClientProperties props, ObjectMapper mapper) {
        JsonSerializer<StockEvento> json = new JsonSerializer<>(mapper);
        json.setAddTypeInfo(false);
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
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
        // Misma partición en la DLT (la DLT tiene las mismas 3 particiones que el tópico original)
        DeadLetterPublishingRecoverer publisher = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(props.consumer().retryMaxAttempts());
        backOff.setInitialInterval(props.consumer().retryBackoffMs());
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterLogger(publisher), backOff);
        // Errores de datos: reintentar no los arregla -> directo a la DLT
        handler.addNotRetryableExceptions(PayloadInvalidoException.class, PedidoNoEncontradoException.class);
        return handler;
    }
}
