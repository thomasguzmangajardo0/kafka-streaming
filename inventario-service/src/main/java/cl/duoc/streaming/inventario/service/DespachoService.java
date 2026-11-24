package cl.duoc.streaming.inventario.service;

import cl.duoc.streaming.inventario.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.inventario.exception.PayloadInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Programa el despacho de los pedidos confirmados. Sin dependencias de RabbitMQ. */
@Service
public class DespachoService {

    public record Despacho(String pedidoId, String clienteEmail, Instant programadoEn) {
    }

    private static final Logger log = LoggerFactory.getLogger(DespachoService.class);
    private static final int MAX_HISTORIAL = 200;

    private final Deque<Despacho> despachos = new ConcurrentLinkedDeque<>();
    private final Set<String> mensajesProcesados = ConcurrentHashMap.newKeySet();

    public void programar(PedidoFinalizadoMensaje mensaje) {
        if (mensaje == null || mensaje.pedidoId() == null || mensaje.mensajeId() == null
                || !PedidoFinalizadoMensaje.CONFIRMADO.equals(mensaje.estado())) {
            throw new PayloadInvalidoException("Mensaje de despacho inválido: " + mensaje);
        }
        if (!mensajesProcesados.add(mensaje.mensajeId())) {
            log.warn("Mensaje {} ya procesado, se ignora (entrega duplicada)", mensaje.mensajeId());
            return;
        }
        despachos.addLast(new Despacho(mensaje.pedidoId(), mensaje.clienteEmail(), Instant.now()));
        while (despachos.size() > MAX_HISTORIAL) {
            despachos.pollFirst();
        }
        log.info("[DESPACHO] Pedido {} programado para despacho a {}", mensaje.pedidoId(), mensaje.clienteEmail());
    }

    public List<Despacho> listar() {
        List<Despacho> copia = new ArrayList<>(despachos);
        Collections.reverse(copia);
        return copia;
    }
}
