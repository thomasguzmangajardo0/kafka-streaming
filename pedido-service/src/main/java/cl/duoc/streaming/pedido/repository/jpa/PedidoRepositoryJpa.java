package cl.duoc.streaming.pedido.repository.jpa;

import cl.duoc.streaming.pedido.domain.Pedido;
import cl.duoc.streaming.pedido.dto.ItemPedido;
import cl.duoc.streaming.pedido.repository.PedidoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Implementación del puerto {@link PedidoRepository} sobre Spring Data JPA. */
@Repository
public class PedidoRepositoryJpa implements PedidoRepository {

    private final PedidoJpaRepository jpa;

    public PedidoRepositoryJpa(PedidoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pedido guardar(Pedido pedido) {
        return aDominio(jpa.save(aEntidad(pedido)));
    }

    @Override
    public Optional<Pedido> buscar(String id) {
        return jpa.findById(id).map(PedidoRepositoryJpa::aDominio);
    }

    @Override
    public Collection<Pedido> listar() {
        return jpa.findAllByOrderByCreadoEnDesc().stream().map(PedidoRepositoryJpa::aDominio).toList();
    }

    @Override
    public void eliminar(String id) {
        jpa.deleteById(id);
    }

    static PedidoEntity aEntidad(Pedido p) {
        List<ItemPedidoEmbeddable> items = p.items().stream()
                .map(i -> new ItemPedidoEmbeddable(i.sku(), i.cantidad(), i.precioUnitario()))
                .toList();
        return new PedidoEntity(p.id(), p.clienteId(), p.clienteEmail(), p.total(), p.estado(), p.motivo(),
                p.creadoEn(), p.actualizadoEn(), items);
    }

    static Pedido aDominio(PedidoEntity e) {
        List<ItemPedido> items = e.getItems().stream()
                .map(i -> new ItemPedido(i.getSku(), i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        return new Pedido(e.getId(), e.getClienteId(), e.getClienteEmail(), items, e.getTotal(), e.getEstado(),
                e.getMotivo(), e.getCreadoEn(), e.getActualizadoEn());
    }
}
