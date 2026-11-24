package cl.duoc.streaming.pedido.messaging;

import cl.duoc.streaming.pedido.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.pedido.exception.EventoNoPublicadoException;
import cl.duoc.streaming.pedido.service.PedidoFinalizadoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica en RabbitMQ el resultado final del pedido:
 * <ul>
 *   <li>exchange <b>direct</b> {@code pedidos.direct}, clave {@code pedido.confirmado}
 *       (solo confirmados) => cola {@code cola.pedidos.confirmados} (despacho en inventario);</li>
 *   <li>exchange <b>topic</b> {@code eventos.topic}, clave {@code pedido.confirmado} o
 *       {@code pedido.rechazado} => colas de notificaciones ({@code pedido.*}) y auditoría ({@code pedido.#}).</li>
 * </ul>
 * Kafka sigue transportando el flujo de eventos; RabbitMQ entrega estas tareas puntuales a cada consumidor.
 */
@Component
public class RabbitPedidoFinalizadoPublisher implements PedidoFinalizadoPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitPedidoFinalizadoPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeDirect;
    private final String exchangeTopic;

    public RabbitPedidoFinalizadoPublisher(RabbitTemplate rabbitTemplate,
                                           @Value("${app.rabbit.exchanges.direct}") String exchangeDirect,
                                           @Value("${app.rabbit.exchanges.topic}") String exchangeTopic) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeDirect = exchangeDirect;
        this.exchangeTopic = exchangeTopic;
        // Mensajes que ningún binding pudo enrutar: se dejan en el log para no perderlos de vista
        this.rabbitTemplate.setReturnsCallback(r -> log.error(
                "[RABBIT] Mensaje no enrutable: exchange={} routingKey={} motivo={}",
                r.getExchange(), r.getRoutingKey(), r.getReplyText()));
    }

    @Override
    public void publicar(PedidoFinalizadoMensaje mensaje) {
        String routingKey = "pedido." + mensaje.estado().toLowerCase();
        try {
            if (PedidoFinalizadoMensaje.CONFIRMADO.equals(mensaje.estado())) {
                rabbitTemplate.convertAndSend(exchangeDirect, routingKey, mensaje);
            }
            rabbitTemplate.convertAndSend(exchangeTopic, routingKey, mensaje);
            log.info("[RABBIT] Publicado {} del pedido {} (routingKey={})", mensaje.estado(), mensaje.pedidoId(), routingKey);
        } catch (AmqpException e) {
            throw new EventoNoPublicadoException("No se pudo publicar en RabbitMQ el pedido " + mensaje.pedidoId(), e);
        }
    }
}
