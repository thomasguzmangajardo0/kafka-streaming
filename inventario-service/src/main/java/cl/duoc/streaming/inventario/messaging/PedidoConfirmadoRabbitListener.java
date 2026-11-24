package cl.duoc.streaming.inventario.messaging;

import cl.duoc.streaming.inventario.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.inventario.service.DespachoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada RabbitMQ: recibe pedidos confirmados y delega en {@link DespachoService}. */
@Component
public class PedidoConfirmadoRabbitListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoConfirmadoRabbitListener.class);

    private final DespachoService despachoService;

    public PedidoConfirmadoRabbitListener(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @RabbitListener(queues = "${app.rabbit.queues.pedidos-confirmados}")
    public void onPedidoConfirmado(PedidoFinalizadoMensaje mensaje) {
        log.info("[RABBIT] Recibido pedido confirmado {}", mensaje == null ? null : mensaje.pedidoId());
        despachoService.programar(mensaje);
    }
}
