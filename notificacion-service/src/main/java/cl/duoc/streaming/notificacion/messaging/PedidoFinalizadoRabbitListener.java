package cl.duoc.streaming.notificacion.messaging;

import cl.duoc.streaming.notificacion.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.notificacion.service.AuditoriaService;
import cl.duoc.streaming.notificacion.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada RabbitMQ. Dos colas ligadas al exchange topic {@code eventos.topic}:
 * {@code cola.notificaciones} (clave {@code pedido.*}) y {@code cola.auditoria} (clave {@code pedido.#}).
 * Solo recibe y delega: la lógica vive en los servicios.
 */
@Component
public class PedidoFinalizadoRabbitListener {

    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public PedidoFinalizadoRabbitListener(NotificacionService notificacionService, AuditoriaService auditoriaService) {
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    @RabbitListener(queues = "${app.rabbit.queues.notificaciones}")
    public void onNotificacion(PedidoFinalizadoMensaje mensaje) {
        notificacionService.notificarEstadoFinal(mensaje);
    }

    @RabbitListener(queues = "${app.rabbit.queues.auditoria}")
    public void onAuditoria(PedidoFinalizadoMensaje mensaje,
                            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        auditoriaService.registrar(mensaje, routingKey);
    }
}
