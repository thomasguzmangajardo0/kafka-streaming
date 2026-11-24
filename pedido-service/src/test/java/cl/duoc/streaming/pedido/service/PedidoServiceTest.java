package cl.duoc.streaming.pedido.service;

import cl.duoc.streaming.pedido.domain.EstadoPedido;
import cl.duoc.streaming.pedido.domain.Pedido;
import cl.duoc.streaming.pedido.dto.CrearPedidoRequest;
import cl.duoc.streaming.pedido.dto.EstadoStock;
import cl.duoc.streaming.pedido.dto.PedidoEvento;
import cl.duoc.streaming.pedido.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.pedido.dto.StockEvento;
import cl.duoc.streaming.pedido.exception.EventoNoPublicadoException;
import cl.duoc.streaming.pedido.exception.PayloadInvalidoException;
import cl.duoc.streaming.pedido.exception.PedidoNoEncontradoException;
import cl.duoc.streaming.pedido.repository.InMemoryPedidoRepository;
import cl.duoc.streaming.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PedidoServiceTest {

    private final List<PedidoEvento> publicados = new ArrayList<>();
    private final List<PedidoFinalizadoMensaje> finalizados = new ArrayList<>();
    private PedidoRepository repository;
    private PedidoService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPedidoRepository();
        service = new PedidoService(repository, publicados::add, finalizados::add);
    }

    private CrearPedidoRequest request() {
        return new CrearPedidoRequest("cli-1", "cli@duoc.cl", List.of(
                new CrearPedidoRequest.ItemRequest("SKU-001", 2, new BigDecimal("1500")),
                new CrearPedidoRequest.ItemRequest("SKU-002", 1, new BigDecimal("990"))));
    }

    @Test
    void crearCalculaTotalGuardaPendientePublicaEvento() {
        Pedido p = service.crear(request());

        assertEquals(new BigDecimal("3990"), p.total());
        assertEquals(EstadoPedido.PENDIENTE, p.estado());
        assertEquals(1, publicados.size());
        PedidoEvento ev = publicados.get(0);
        assertEquals(PedidoEvento.TIPO_PEDIDO_CREADO, ev.tipoEvento());
        assertEquals(p.id(), ev.pedidoId());
        assertEquals(2, ev.items().size());
    }

    @Test
    void siFallaLaPublicacionNoQuedaPedidoGuardado() {
        PedidoService fallido = new PedidoService(repository, e -> {
            throw new EventoNoPublicadoException("broker caído", null);
        }, finalizados::add);
        assertThrows(EventoNoPublicadoException.class, () -> fallido.crear(request()));
        assertTrue(repository.listar().isEmpty());
    }

    @Test
    void stockReservadoConfirmaElPedido() {
        Pedido p = service.crear(request());
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RESERVADO, null));
        assertEquals(EstadoPedido.CONFIRMADO, service.obtener(p.id()).estado());
    }

    @Test
    void stockRechazadoRechazaElPedidoConMotivo() {
        Pedido p = service.crear(request());
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RECHAZADO, "Sin stock"));
        Pedido actualizado = service.obtener(p.id());
        assertEquals(EstadoPedido.RECHAZADO, actualizado.estado());
        assertEquals("Sin stock", actualizado.motivo());
    }

    @Test
    void eventoDuplicadoNoCambiaUnPedidoYaResuelto() {
        Pedido p = service.crear(request());
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RESERVADO, null));
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RECHAZADO, "tardío"));
        assertEquals(EstadoPedido.CONFIRMADO, service.obtener(p.id()).estado());
    }

    @Test
    void eventoInvalidoOPedidoInexistenteLanzanExcepcionesNoReintentables() {
        assertThrows(PayloadInvalidoException.class, () -> service.aplicarResultadoStock(null));
        assertThrows(PedidoNoEncontradoException.class,
                () -> service.aplicarResultadoStock(stock("no-existe", EstadoStock.RESERVADO, null)));
    }

    @Test
    void confirmacionPublicaMensajeRabbitConEstadoConfirmado() {
        Pedido p = service.crear(request());
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RESERVADO, null));

        assertEquals(1, finalizados.size());
        assertEquals(PedidoFinalizadoMensaje.CONFIRMADO, finalizados.get(0).estado());
        assertEquals(p.id(), finalizados.get(0).pedidoId());
        assertEquals("cli@duoc.cl", finalizados.get(0).clienteEmail());
    }

    @Test
    void rechazoPublicaMensajeRabbitConMotivo() {
        Pedido p = service.crear(request());
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RECHAZADO, "Sin stock"));

        assertEquals(PedidoFinalizadoMensaje.RECHAZADO, finalizados.get(0).estado());
        assertEquals("Sin stock", finalizados.get(0).motivo());
    }

    @Test
    void eventoDuplicadoNoVuelveAPublicarEnRabbit() {
        Pedido p = service.crear(request());
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RESERVADO, null));
        service.aplicarResultadoStock(stock(p.id(), EstadoStock.RESERVADO, null));
        assertEquals(1, finalizados.size());
    }

    @Test
    void siRabbitFallaElPedidoSigueEnPendienteParaReintentar() {
        PedidoService conRabbitCaido = new PedidoService(repository, publicados::add, m -> {
            throw new EventoNoPublicadoException("rabbit caído", null);
        });
        Pedido p = conRabbitCaido.crear(request());
        assertThrows(EventoNoPublicadoException.class,
                () -> conRabbitCaido.aplicarResultadoStock(stock(p.id(), EstadoStock.RESERVADO, null)));
        assertEquals(EstadoPedido.PENDIENTE, conRabbitCaido.obtener(p.id()).estado());
    }

    private StockEvento stock(String pedidoId, EstadoStock estado, String motivo) {
        return new StockEvento("e-1", "STOCK", pedidoId, "cli@duoc.cl", estado, motivo, List.of(), Instant.now());
    }
}
