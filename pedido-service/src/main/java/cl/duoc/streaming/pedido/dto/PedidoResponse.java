package cl.duoc.streaming.pedido.dto;

import cl.duoc.streaming.pedido.domain.EstadoPedido;
import cl.duoc.streaming.pedido.domain.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
        String id,
        String clienteId,
        String clienteEmail,
        List<ItemPedido> items,
        BigDecimal total,
        EstadoPedido estado,
        String motivo,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.id(), p.clienteId(), p.clienteEmail(), p.items(), p.total(),
                p.estado(), p.motivo(), p.creadoEn(), p.actualizadoEn());
    }
}
