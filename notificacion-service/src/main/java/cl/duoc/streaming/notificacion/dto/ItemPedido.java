package cl.duoc.streaming.notificacion.dto;

import java.math.BigDecimal;

/** Línea de un pedido. Forma parte de los eventos de Kafka. */
public record ItemPedido(String sku, int cantidad, BigDecimal precioUnitario) {
}
