package cl.duoc.streaming.inventario.messaging;

import cl.duoc.streaming.inventario.dto.EstadoStock;
import cl.duoc.streaming.inventario.dto.StockEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Prueba de integración con un broker Kafka embebido: valida el flujo real
 * pedidos.eventos -> inventario-service -> inventario.eventos y el envío a la DLT.
 */
@SpringBootTest(properties = {
        "app.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.consumer.retry-backoff-ms=100",
        "spring.rabbitmq.listener.simple.auto-startup=false" // esta prueba solo valida Kafka: sin broker RabbitMQ
})
@EmbeddedKafka(partitions = 3, topics = {"pedidos.eventos", "inventario.eventos", "pedidos.eventos.DLT"})
@DirtiesContext
class InventarioKafkaFlowTest {

    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Autowired
    EmbeddedKafkaBroker broker;

    @Autowired
    ObjectMapper mapper;

    private KafkaTemplate<String, String> productorDePrueba() {
        Map<String, Object> cfg = KafkaTestUtils.producerProps(broker);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), new StringSerializer()));
    }

    private Consumer<String, String> consumidorDePrueba(String grupo, String topico) {
        Map<String, Object> cfg = KafkaTestUtils.consumerProps(grupo, "true", broker);
        cfg.put("auto.offset.reset", "earliest");
        Consumer<String, String> c = new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        c.subscribe(java.util.List.of(topico));
        return c;
    }

    private String pedidoJson(String pedidoId, String sku, int cantidad) {
        return """
                {"eventId":"ev-%s","tipoEvento":"PEDIDO_CREADO","pedidoId":"%s","clienteId":"c-1",
                 "clienteEmail":"c@duoc.cl","items":[{"sku":"%s","cantidad":%d,"precioUnitario":100}],
                 "total":100,"fechaCreacion":"2026-01-01T10:00:00Z"}""".formatted(pedidoId, pedidoId, sku, cantidad);
    }

    @Test
    void pedidoValidoGeneraStockReservado() throws Exception {
        try (Consumer<String, String> resultados = consumidorDePrueba("test-resultados-ok", "inventario.eventos")) {
            productorDePrueba().send(new ProducerRecord<>("pedidos.eventos", "p-ok", pedidoJson("p-ok", "SKU-001", 2)));

            ConsumerRecord<String, String> rec = KafkaTestUtils.getSingleRecord(resultados, "inventario.eventos", ESPERA);
            StockEvento ev = mapper.readValue(rec.value(), StockEvento.class);

            assertEquals("p-ok", rec.key());
            assertEquals("p-ok", ev.pedidoId());
            assertEquals(EstadoStock.RESERVADO, ev.estado());
        }
    }

    @Test
    void mensajeCorruptoTerminaEnLaDlt() {
        try (Consumer<String, String> dlt = consumidorDePrueba("test-dlt", "pedidos.eventos.DLT")) {
            productorDePrueba().send(new ProducerRecord<>("pedidos.eventos", "p-malo", "{esto no es json"));

            ConsumerRecord<String, String> rec = KafkaTestUtils.getSingleRecord(dlt, "pedidos.eventos.DLT", ESPERA);
            assertEquals("{esto no es json", rec.value());
        }
    }
}
