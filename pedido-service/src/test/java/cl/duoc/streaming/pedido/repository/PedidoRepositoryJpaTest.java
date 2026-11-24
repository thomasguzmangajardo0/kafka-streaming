package cl.duoc.streaming.pedido.repository;

import cl.duoc.streaming.pedido.domain.EstadoPedido;
import cl.duoc.streaming.pedido.domain.Pedido;
import cl.duoc.streaming.pedido.dto.ItemPedido;
import cl.duoc.streaming.pedido.repository.jpa.PedidoRepositoryJpa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba entidades + repositorio contra una base H2 embebida (en la nube es PostgreSQL/MySQL en RDS). */
@DataJpaTest
@Import(PedidoRepositoryJpa.class)
class PedidoRepositoryJpaTest {

    @Autowired
    PedidoRepository repository;

    private Pedido pedido(String id, Instant creado) {
        return new Pedido(id, "cli-1", "cli@duoc.cl",
                List.of(new ItemPedido("SKU-001", 2, new BigDecimal("1500.00")),
                        new ItemPedido("SKU-002", 1, new BigDecimal("990.00"))),
                new BigDecimal("3990.00"), EstadoPedido.PENDIENTE, null, creado, creado);
    }

    @Test
    void guardaYRecuperaPedidoConSusItems() {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        repository.guardar(pedido("p-1", ahora));

        Pedido leido = repository.buscar("p-1").orElseThrow();
        assertEquals("cli@duoc.cl", leido.clienteEmail());
        assertEquals(2, leido.items().size());
        assertEquals(0, new BigDecimal("3990").compareTo(leido.total()));
        assertEquals(EstadoPedido.PENDIENTE, leido.estado());
    }

    @Test
    void actualizaElEstadoDelMismoPedido() {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Pedido p = repository.guardar(pedido("p-2", ahora));
        repository.guardar(p.conEstado(EstadoPedido.RECHAZADO, "Sin stock"));

        Pedido leido = repository.buscar("p-2").orElseThrow();
        assertEquals(EstadoPedido.RECHAZADO, leido.estado());
        assertEquals("Sin stock", leido.motivo());
        assertEquals(1, repository.listar().size());
    }

    @Test
    void listaPrimeroLosMasRecientesYPermiteEliminar() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        repository.guardar(pedido("viejo", base.minusSeconds(60)));
        repository.guardar(pedido("nuevo", base));

        assertEquals("nuevo", repository.listar().iterator().next().id());
        repository.eliminar("viejo");
        assertTrue(repository.buscar("viejo").isEmpty());
    }
}
