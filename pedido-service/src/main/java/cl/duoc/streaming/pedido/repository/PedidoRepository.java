package cl.duoc.streaming.pedido.repository;

import cl.duoc.streaming.pedido.domain.Pedido;

import java.util.Collection;
import java.util.Optional;

/** Puerto de persistencia: el servicio de negocio no conoce JPA ni la base de datos. */
public interface PedidoRepository {

    Pedido guardar(Pedido pedido);

    Optional<Pedido> buscar(String id);

    /** Pedidos más recientes primero. */
    Collection<Pedido> listar();

    void eliminar(String id);
}
