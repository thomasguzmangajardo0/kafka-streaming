package cl.duoc.streaming.notificacion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento publicado en {@code pedidos.eventos} (clave = pedidoId).
 * Es autocontenido: inventario y notificaciones no necesitan consultar al servicio de pedidos.
 */
public record PedidoEvento(
        String eventId,
        String tipoEvento,
        String pedidoId,
        String clienteId,
        String clienteEmail,
        List<ItemPedido> items,
        BigDecimal total,
        Instant fechaCreacion) {

    public static final String TIPO_PEDIDO_CREADO = "PEDIDO_CREADO";
}
