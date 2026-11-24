package cl.duoc.streaming.pedido.messaging;

import cl.duoc.streaming.pedido.dto.StockEvento;
import cl.duoc.streaming.pedido.service.PedidoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada Kafka: solo recibe y delega. Toda la lógica vive en {@link PedidoService}.
 * Grupo {@code pedido-group}: varias instancias del servicio se reparten las particiones.
 */
@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);

    private final PedidoService pedidoService;

    public StockEventListener(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @KafkaListener(
            id = "pedido-stock-listener",
            topics = "${app.kafka.topics.inventario}",
            groupId = "${app.kafka.consumer.group-id}",
            containerFactory = "stockKafkaListenerContainerFactory")
    public void onStockEvento(ConsumerRecord<String, StockEvento> record) {
        log.info("Recibido de {} [partition={}, offset={}] key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        if (record.value() == null) { // tombstone del tópico compactado
            log.debug("Tombstone ignorado key={}", record.key());
            return;
        }
        pedidoService.aplicarResultadoStock(record.value());
    }
}
