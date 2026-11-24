package cl.duoc.streaming.inventario.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import cl.duoc.streaming.inventario.messaging.DlqLoggingRecoverer;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

/** Serialización JSON de los mensajes RabbitMQ con el ObjectMapper de Spring Boot. */
@Configuration
public class RabbitMessagingConfig {

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper mapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        // El consumidor usa el tipo del parámetro del listener, sin depender del __TypeId__ del productor
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    /** Tras agotar los reintentos: registra en el log y rechaza sin reencolar => la cola envía el mensaje a su DLQ. */
    @Bean
    public MessageRecoverer mensajeFallidoRecoverer() {
        return new DlqLoggingRecoverer();
    }
}
