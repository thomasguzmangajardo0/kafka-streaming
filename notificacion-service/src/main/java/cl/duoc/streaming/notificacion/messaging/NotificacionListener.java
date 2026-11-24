package cl.duoc.streaming.notificacion.messaging;

import cl.duoc.streaming.notificacion.dto.PedidoEvento;
import cl.duoc.streaming.notificacion.dto.StockEvento;
import cl.duoc.streaming.notificacion.service.NotificacionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptadores de entrada Kafka: reciben, registran metadatos y delegan en {@link NotificacionService}. */
@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    private final NotificacionService service;

    public NotificacionListener(NotificacionService service) {
        this.service = service;
    }

    @KafkaListener(
            id = "notificacion-pedidos-listener",
            topics = "${app.kafka.topics.pedidos}",
            groupId = "${app.kafka.consumer.pedidos-group-id}",
            containerFactory = "pedidoKafkaListenerContainerFactory")
    public void onPedido(ConsumerRecord<String, PedidoEvento> record) {
        log.info("Recibido de {} [partition={}, offset={}] key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        service.notificarPedidoCreado(record.value());
    }

    @KafkaListener(
            id = "notificacion-stock-listener",
            topics = "${app.kafka.topics.inventario}",
            groupId = "${app.kafka.consumer.stock-group-id}",
            containerFactory = "stockKafkaListenerContainerFactory")
    public void onStock(ConsumerRecord<String, StockEvento> record) {
        log.info("Recibido de {} [partition={}, offset={}] key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        if (record.value() == null) { // tombstone del tópico compactado
            return;
        }
        service.notificarResultadoStock(record.value());
    }
}
