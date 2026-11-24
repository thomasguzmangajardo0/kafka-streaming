package cl.duoc.streaming.inventario.messaging;

import cl.duoc.streaming.inventario.config.KafkaClientProperties;
import cl.duoc.streaming.inventario.dto.StockEvento;
import cl.duoc.streaming.inventario.exception.EventoNoPublicadoException;
import cl.duoc.streaming.inventario.service.StockEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Productor de resultados de stock. Clave = pedidoId: en el tópico compactado
 * {@code inventario.eventos} se conserva solo el último resultado por pedido.
 */
@Component
public class KafkaStockEventPublisher implements StockEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaStockEventPublisher.class);

    private final KafkaTemplate<String, StockEvento> template;
    private final String topic;

    public KafkaStockEventPublisher(KafkaTemplate<String, StockEvento> stockKafkaTemplate, KafkaClientProperties props) {
        this.template = stockKafkaTemplate;
        this.topic = props.topics().inventario();
    }

    @Override
    public void publicar(StockEvento evento) {
        try {
            SendResult<String, StockEvento> r = template.send(topic, evento.pedidoId(), evento).get(15, TimeUnit.SECONDS);
            log.info("Evento {} publicado en {} [partition={}, offset={}] pedido={}",
                    evento.tipoEvento(), topic, r.getRecordMetadata().partition(), r.getRecordMetadata().offset(),
                    evento.pedidoId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventoNoPublicadoException("Publicación interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new EventoNoPublicadoException("No se pudo publicar el resultado de stock en Kafka", e);
        }
    }
}
