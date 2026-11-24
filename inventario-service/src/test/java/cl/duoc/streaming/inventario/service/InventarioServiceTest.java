package cl.duoc.streaming.inventario.service;

import cl.duoc.streaming.inventario.config.InventarioProperties;
import cl.duoc.streaming.inventario.dto.EstadoStock;
import cl.duoc.streaming.inventario.dto.ItemPedido;
import cl.duoc.streaming.inventario.dto.PedidoEvento;
import cl.duoc.streaming.inventario.dto.StockEvento;
import cl.duoc.streaming.inventario.exception.EventoNoPublicadoException;
import cl.duoc.streaming.inventario.exception.PayloadInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioServiceTest {

    private final List<StockEvento> publicados = new ArrayList<>();
    private InventarioService service;

    @BeforeEach
    void setUp() {
        service = new InventarioService(new InventarioProperties(Map.of("SKU-001", 10, "SKU-002", 1)), publicados::add);
    }

    private PedidoEvento pedido(String eventId, String sku, int cantidad) {
        return new PedidoEvento(eventId, PedidoEvento.TIPO_PEDIDO_CREADO, "p-" + eventId, "c-1", "c@duoc.cl",
                List.of(new ItemPedido(sku, cantidad, BigDecimal.TEN)), BigDecimal.TEN, Instant.now());
    }

    @Test
    void reservaDescuentaStockYPublicaReservado() {
        service.procesarPedido(pedido("e1", "SKU-001", 4));

        assertEquals(6, service.consultarStock().get("SKU-001"));
        assertEquals(1, publicados.size());
        assertEquals(EstadoStock.RESERVADO, publicados.get(0).estado());
        assertEquals("p-e1", publicados.get(0).pedidoId());
        assertEquals("c@duoc.cl", publicados.get(0).clienteEmail());
    }

    @Test
    void stockInsuficienteRechazaSinDescontar() {
        service.procesarPedido(pedido("e1", "SKU-002", 5));

        assertEquals(1, service.consultarStock().get("SKU-002"));
        assertEquals(EstadoStock.RECHAZADO, publicados.get(0).estado());
        assertNotNull(publicados.get(0).motivo());
    }

    @Test
    void skuInexistenteSeRechaza() {
        service.procesarPedido(pedido("e1", "SKU-XXX", 1));
        assertEquals(EstadoStock.RECHAZADO, publicados.get(0).estado());
    }

    @Test
    void entregaDuplicadaNoDescuentaDosVeces() {
        PedidoEvento ev = pedido("e1", "SKU-001", 3);
        service.procesarPedido(ev);
        service.procesarPedido(ev);

        assertEquals(7, service.consultarStock().get("SKU-001"));
        assertEquals(1, publicados.size());
    }

    @Test
    void payloadInvalidoLanzaExcepcionNoReintentable() {
        assertThrows(PayloadInvalidoException.class, () -> service.procesarPedido(null));
        assertThrows(PayloadInvalidoException.class, () -> service.procesarPedido(pedido("e1", "SKU-001", 0)));
        PedidoEvento sinItems = new PedidoEvento("e2", "X", "p2", "c", "c@x.cl", List.of(), BigDecimal.ZERO, Instant.now());
        assertThrows(PayloadInvalidoException.class, () -> service.procesarPedido(sinItems));
    }

    @Test
    void siFallaLaPublicacionSeRevierteLaReservaYSePuedeReintentar() {
        InventarioService fallido = new InventarioService(new InventarioProperties(Map.of("SKU-001", 10)), e -> {
            throw new EventoNoPublicadoException("broker caído", null);
        });
        PedidoEvento ev = pedido("e1", "SKU-001", 4);

        assertThrows(EventoNoPublicadoException.class, () -> fallido.procesarPedido(ev));
        assertEquals(10, fallido.consultarStock().get("SKU-001"));
        // El reintento no se considera duplicado
        assertThrows(EventoNoPublicadoException.class, () -> fallido.procesarPedido(ev));
    }
}
