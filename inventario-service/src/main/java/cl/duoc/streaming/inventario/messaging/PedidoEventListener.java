package cl.duoc.streaming.inventario.messaging;

import cl.duoc.streaming.inventario.dto.PedidoEvento;
import cl.duoc.streaming.inventario.service.InventarioService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada Kafka: recibe PEDIDO_CREADO y delega en {@link InventarioService}. */
@Component
public class PedidoEventListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventListener.class);

    private final InventarioService inventarioService;

    public PedidoEventListener(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @KafkaListener(
            id = "inventario-pedidos-listener",
            topics = "${app.kafka.topics.pedidos}",
            groupId = "${app.kafka.consumer.group-id}",
            containerFactory = "pedidoKafkaListenerContainerFactory")
    public void onPedido(ConsumerRecord<String, PedidoEvento> record) {
        log.info("Recibido de {} [partition={}, offset={}] key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        inventarioService.procesarPedido(record.value());
    }
}
