package cl.duoc.streaming.inventario.service;

import cl.duoc.streaming.inventario.dto.ItemPedido;
import cl.duoc.streaming.inventario.dto.PedidoEvento;
import cl.duoc.streaming.inventario.exception.PayloadInvalidoException;

/** Valida el contrato del evento antes de procesarlo; los errores van directo a la DLT. */
public final class PedidoEventoValidator {

    private PedidoEventoValidator() {
    }

    public static void validar(PedidoEvento ev) {
        if (ev == null) {
            throw new PayloadInvalidoException("Evento nulo");
        }
        if (esVacio(ev.eventId()) || esVacio(ev.pedidoId())) {
            throw new PayloadInvalidoException("Evento sin eventId o pedidoId");
        }
        if (ev.items() == null || ev.items().isEmpty()) {
            throw new PayloadInvalidoException("Pedido " + ev.pedidoId() + " sin items");
        }
        for (ItemPedido item : ev.items()) {
            if (item == null || esVacio(item.sku()) || item.cantidad() <= 0) {
                throw new PayloadInvalidoException("Item inválido en pedido " + ev.pedidoId() + ": " + item);
            }
        }
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }
}
