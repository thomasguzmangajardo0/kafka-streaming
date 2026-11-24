package cl.duoc.streaming.inventario.service;

import cl.duoc.streaming.inventario.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.inventario.exception.PayloadInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DespachoServiceTest {

    private final DespachoService service = new DespachoService();

    private PedidoFinalizadoMensaje msg(String id, String estado) {
        return new PedidoFinalizadoMensaje(id, "p-1", "cli@duoc.cl", estado, new BigDecimal("1000"), null, Instant.now());
    }

    @Test
    void programaDespachoDePedidoConfirmado() {
        service.programar(msg("m-1", PedidoFinalizadoMensaje.CONFIRMADO));
        assertEquals(1, service.listar().size());
        assertEquals("p-1", service.listar().get(0).pedidoId());
    }

    @Test
    void ignoraMensajeDuplicado() {
        service.programar(msg("m-1", PedidoFinalizadoMensaje.CONFIRMADO));
        service.programar(msg("m-1", PedidoFinalizadoMensaje.CONFIRMADO));
        assertEquals(1, service.listar().size());
    }

    @Test
    void mensajeInvalidoLanzaExcepcionParaIrALaDlq() {
        assertThrows(PayloadInvalidoException.class, () -> service.programar(null));
        assertThrows(PayloadInvalidoException.class, () -> service.programar(msg("m-2", PedidoFinalizadoMensaje.RECHAZADO)));
        assertThrows(PayloadInvalidoException.class, () -> service.programar(msg(null, PedidoFinalizadoMensaje.CONFIRMADO)));
    }
}
