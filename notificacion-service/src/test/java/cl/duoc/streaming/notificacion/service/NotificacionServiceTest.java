package cl.duoc.streaming.notificacion.service;

import cl.duoc.streaming.notificacion.domain.Notificacion;
import cl.duoc.streaming.notificacion.dto.EstadoStock;
import cl.duoc.streaming.notificacion.dto.PedidoEvento;
import cl.duoc.streaming.notificacion.dto.StockEvento;
import cl.duoc.streaming.notificacion.exception.PayloadInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionServiceTest {

    private final NotificacionService service = new NotificacionService();

    private StockEvento stock(EstadoStock estado, String motivo) {
        return new StockEvento("e1", "STOCK", "p-1", "c@duoc.cl", estado, motivo, List.of(), Instant.now());
    }

    @Test
    void pedidoCreadoGeneraNotificacionDeRecepcion() {
        PedidoEvento ev = new PedidoEvento("e1", "PEDIDO_CREADO", "p-1", "c-1", "c@duoc.cl",
                List.of(), new BigDecimal("3990"), Instant.now());

        Notificacion n = service.notificarPedidoCreado(ev);

        assertEquals("PEDIDO_RECIBIDO", n.tipo());
        assertEquals("c@duoc.cl", n.destinatario());
        assertTrue(n.mensaje().contains("3990"));
    }

    @Test
    void stockReservadoConfirmaYRechazadoInformaMotivo() {
        assertEquals("PEDIDO_CONFIRMADO", service.notificarResultadoStock(stock(EstadoStock.RESERVADO, null)).tipo());
        Notificacion rechazo = service.notificarResultadoStock(stock(EstadoStock.RECHAZADO, "Sin stock"));
        assertEquals("PEDIDO_RECHAZADO", rechazo.tipo());
        assertTrue(rechazo.mensaje().contains("Sin stock"));
    }

    @Test
    void historialDevuelveLasMasRecientesPrimero() {
        service.notificarResultadoStock(stock(EstadoStock.RESERVADO, null));
        service.notificarResultadoStock(stock(EstadoStock.RECHAZADO, "x"));
        assertEquals("PEDIDO_RECHAZADO", service.listar().get(0).tipo());
    }

    @Test
    void eventosIncompletosLanzanExcepcionNoReintentable() {
        assertThrows(PayloadInvalidoException.class, () -> service.notificarPedidoCreado(null));
        assertThrows(PayloadInvalidoException.class, () -> service.notificarResultadoStock(null));
    }
}
