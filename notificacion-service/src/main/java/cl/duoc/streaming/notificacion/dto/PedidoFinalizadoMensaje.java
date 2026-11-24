package cl.duoc.streaming.notificacion.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mensaje RabbitMQ que informa el resultado final de un pedido (CONFIRMADO o RECHAZADO).
 * Se publica en los exchanges {@code pedidos.direct} y {@code eventos.topic}.
 */
public record PedidoFinalizadoMensaje(
        String mensajeId,
        String pedidoId,
        String clienteEmail,
        String estado,
        BigDecimal total,
        String motivo,
        Instant ocurridoEn) {

    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String RECHAZADO = "RECHAZADO";
}
