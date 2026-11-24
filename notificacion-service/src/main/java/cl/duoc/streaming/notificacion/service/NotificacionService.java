package cl.duoc.streaming.notificacion.service;

import cl.duoc.streaming.notificacion.domain.Notificacion;
import cl.duoc.streaming.notificacion.dto.EstadoStock;
import cl.duoc.streaming.notificacion.dto.PedidoEvento;
import cl.duoc.streaming.notificacion.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.notificacion.dto.StockEvento;
import cl.duoc.streaming.notificacion.exception.PayloadInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Construye y registra notificaciones. Sin dependencias de Kafka. */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final int MAX_HISTORIAL = 200;

    private final Deque<Notificacion> historial = new ConcurrentLinkedDeque<>();

    public Notificacion notificarPedidoCreado(PedidoEvento ev) {
        if (ev == null || ev.pedidoId() == null || ev.clienteEmail() == null) {
            throw new PayloadInvalidoException("PedidoEvento sin pedidoId o clienteEmail: " + ev);
        }
        return registrar(new Notificacion(UUID.randomUUID().toString(), ev.pedidoId(), ev.clienteEmail(),
                "PEDIDO_RECIBIDO", "Recibimos tu pedido " + ev.pedidoId(),
                "Estamos validando el stock de tu pedido por un total de $" + ev.total() + ".", Instant.now()));
    }

    public Notificacion notificarResultadoStock(StockEvento ev) {
        if (ev == null || ev.pedidoId() == null || ev.clienteEmail() == null || ev.estado() == null) {
            throw new PayloadInvalidoException("StockEvento incompleto: " + ev);
        }
        boolean ok = ev.estado() == EstadoStock.RESERVADO;
        return registrar(new Notificacion(UUID.randomUUID().toString(), ev.pedidoId(), ev.clienteEmail(),
                ok ? "PEDIDO_CONFIRMADO" : "PEDIDO_RECHAZADO",
                ok ? "Tu pedido " + ev.pedidoId() + " fue confirmado" : "No pudimos procesar tu pedido " + ev.pedidoId(),
                ok ? "Reservamos el stock y prepararemos tu pedido." : "Motivo: " + ev.motivo(), Instant.now()));
    }

    /** Notificación por el canal RabbitMQ: estado final del pedido (cola.notificaciones). */
    public Notificacion notificarEstadoFinal(PedidoFinalizadoMensaje msg) {
        if (msg == null || msg.pedidoId() == null || msg.clienteEmail() == null || msg.estado() == null) {
            throw new PayloadInvalidoException("PedidoFinalizadoMensaje incompleto: " + msg);
        }
        boolean ok = PedidoFinalizadoMensaje.CONFIRMADO.equals(msg.estado());
        return registrar(new Notificacion(UUID.randomUUID().toString(), msg.pedidoId(), msg.clienteEmail(),
                ok ? "PEDIDO_EN_DESPACHO" : "PEDIDO_CANCELADO",
                ok ? "Tu pedido " + msg.pedidoId() + " va en camino" : "Tu pedido " + msg.pedidoId() + " fue cancelado",
                ok ? "Total pagado: $" + msg.total() : "Motivo: " + msg.motivo(), Instant.now()));
    }

    public List<Notificacion> listar() {
        List<Notificacion> copia = new ArrayList<>(historial);
        Collections.reverse(copia); // más recientes primero
        return copia;
    }

    private Notificacion registrar(Notificacion n) {
        historial.addLast(n);
        while (historial.size() > MAX_HISTORIAL) {
            historial.pollFirst();
        }
        log.info("[EMAIL simulado] to={} tipo={} asunto=\"{}\"", n.destinatario(), n.tipo(), n.asunto());
        return n;
    }
}
