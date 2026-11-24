package cl.duoc.streaming.pedido.repository;

import cl.duoc.streaming.pedido.domain.Pedido;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Doble de prueba del puerto de persistencia (pruebas unitarias sin base de datos). */
public class InMemoryPedidoRepository implements PedidoRepository {

    private final Map<String, Pedido> pedidos = new ConcurrentHashMap<>();

    @Override
    public Pedido guardar(Pedido pedido) {
        pedidos.put(pedido.id(), pedido);
        return pedido;
    }

    @Override
    public Optional<Pedido> buscar(String id) {
        return Optional.ofNullable(pedidos.get(id));
    }

    @Override
    public Collection<Pedido> listar() {
        return pedidos.values();
    }

    @Override
    public void eliminar(String id) {
        pedidos.remove(id);
    }
}
