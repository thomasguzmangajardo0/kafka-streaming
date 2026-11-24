package cl.duoc.streaming.pedido.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, String> {

    List<PedidoEntity> findAllByOrderByCreadoEnDesc();
}
