package cl.duoc.streaming.notificacion.service;

import cl.duoc.streaming.notificacion.domain.Notificacion;
import cl.duoc.streaming.notificacion.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.notificacion.exception.PayloadInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitServicesTest {

    private PedidoFinalizadoMensaje msg(String estado) {
        return new PedidoFinalizadoMensaje("m-1", "p-1", "cli@duoc.cl", estado, new BigDecimal("2500"), "Sin stock", Instant.now());
    }

    @Test
    void notificaPedidoEnDespachoCuandoEstaConfirmado() {
        Notificacion n = new NotificacionService().notificarEstadoFinal(msg(PedidoFinalizadoMensaje.CONFIRMADO));
        assertEquals("PEDIDO_EN_DESPACHO", n.tipo());
        assertEquals("cli@duoc.cl", n.destinatario());
    }

    @Test
    void notificaCancelacionConMotivoCuandoFueRechazado() {
        Notificacion n = new NotificacionService().notificarEstadoFinal(msg(PedidoFinalizadoMensaje.RECHAZADO));
        assertEquals("PEDIDO_CANCELADO", n.tipo());
        assertEquals("Motivo: Sin stock", n.mensaje());
    }

    @Test
    void mensajeIncompletoLanzaExcepcionParaIrALaDlq() {
        assertThrows(PayloadInvalidoException.class, () -> new NotificacionService().notificarEstadoFinal(null));
        assertThrows(PayloadInvalidoException.class, () -> new AuditoriaService().registrar(null, "pedido.confirmado"));
    }

    @Test
    void auditoriaRegistraConLaRoutingKeyRecibida() {
        AuditoriaService auditoria = new AuditoriaService();
        auditoria.registrar(msg(PedidoFinalizadoMensaje.CONFIRMADO), "pedido.confirmado");
        assertEquals(1, auditoria.listar().size());
        assertEquals("pedido.confirmado", auditoria.listar().get(0).routingKey());
    }
}
