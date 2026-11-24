package cl.duoc.streaming.pedido.messaging;

import cl.duoc.streaming.pedido.config.KafkaClientProperties;
import cl.duoc.streaming.pedido.dto.PedidoEvento;
import cl.duoc.streaming.pedido.exception.EventoNoPublicadoException;
import cl.duoc.streaming.pedido.service.PedidoEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Productor Kafka. La clave es el pedidoId: todos los eventos de un pedido caen en la misma
 * partición y por lo tanto se consumen en orden.
 */
@Component
public class KafkaPedidoEventPublisher implements PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPedidoEventPublisher.class);

    private final KafkaTemplate<String, PedidoEvento> template;
    private final String topic;

    public KafkaPedidoEventPublisher(KafkaTemplate<String, PedidoEvento> pedidoKafkaTemplate,
                                     KafkaClientProperties props) {
        this.template = pedidoKafkaTemplate;
        this.topic = props.topics().pedidos();
    }

    @Override
    public void publicar(PedidoEvento evento) {
        try {
            SendResult<String, PedidoEvento> r = template.send(topic, evento.pedidoId(), evento).get(15, TimeUnit.SECONDS);
            log.info("Evento {} publicado en {} [partition={}, offset={}] pedido={}",
                    evento.tipoEvento(), topic, r.getRecordMetadata().partition(), r.getRecordMetadata().offset(),
                    evento.pedidoId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventoNoPublicadoException("Publicación interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("No se pudo publicar el evento del pedido {} en {}: {}", evento.pedidoId(), topic, e.getMessage());
            throw new EventoNoPublicadoException("No se pudo publicar el evento en Kafka", e);
        }
    }
}
