package cl.duoc.streaming.pedido.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/** Línea de pedido persistida en la tabla {@code pedido_items}. */
@Embeddable
public class ItemPedidoEmbeddable {

    @Column(nullable = false, length = 60)
    private String sku;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    protected ItemPedidoEmbeddable() {
        // requerido por JPA
    }

    public ItemPedidoEmbeddable(String sku, int cantidad, BigDecimal precioUnitario) {
        this.sku = sku;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getSku() {
        return sku;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
}
