package cl.duoc.streaming.pedido.dto;

import java.time.Instant;
import java.util.List;

/**
 * Evento publicado por inventario en {@code inventario.eventos} (clave = pedidoId).
 * Resultado de la reserva de stock para un pedido.
 */
public record StockEvento(
        String eventId,
        String tipoEvento,
        String pedidoId,
        String clienteEmail,
        EstadoStock estado,
        String motivo,
        List<ItemPedido> items,
        Instant fechaEvento) {
}
