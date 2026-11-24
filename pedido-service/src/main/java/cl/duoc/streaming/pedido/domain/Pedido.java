package cl.duoc.streaming.pedido.domain;

import cl.duoc.streaming.pedido.dto.ItemPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Pedido inmutable; los cambios de estado generan una copia. */
public record Pedido(
        String id,
        String clienteId,
        String clienteEmail,
        List<ItemPedido> items,
        BigDecimal total,
        EstadoPedido estado,
        String motivo,
        Instant creadoEn,
        Instant actualizadoEn) {

    public Pedido conEstado(EstadoPedido nuevoEstado, String nuevoMotivo) {
        return new Pedido(id, clienteId, clienteEmail, items, total, nuevoEstado, nuevoMotivo, creadoEn, Instant.now());
    }
}
