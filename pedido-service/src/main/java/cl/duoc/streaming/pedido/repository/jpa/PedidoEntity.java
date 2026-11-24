package cl.duoc.streaming.pedido.repository.jpa;

import cl.duoc.streaming.pedido.domain.EstadoPedido;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fila de la tabla {@code pedidos} en la base de datos cloud (Amazon RDS). */
@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "cliente_id", nullable = false, length = 100)
    private String clienteId;

    @Column(name = "cliente_email", nullable = false, length = 200)
    private String clienteEmail;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(length = 500)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_items", joinColumns = @JoinColumn(name = "pedido_id"))
    private List<ItemPedidoEmbeddable> items = new ArrayList<>();

    protected PedidoEntity() {
        // requerido por JPA
    }

    public PedidoEntity(String id, String clienteId, String clienteEmail, BigDecimal total, EstadoPedido estado,
                        String motivo, Instant creadoEn, Instant actualizadoEn, List<ItemPedidoEmbeddable> items) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteEmail = clienteEmail;
        this.total = total;
        this.estado = estado;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
        this.items = new ArrayList<>(items);
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public List<ItemPedidoEmbeddable> getItems() {
        return items;
    }
}
