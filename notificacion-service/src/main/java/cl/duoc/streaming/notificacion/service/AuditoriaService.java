package cl.duoc.streaming.notificacion.service;

import cl.duoc.streaming.notificacion.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.notificacion.exception.PayloadInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Registro de auditoría de los pedidos finalizados (cola.auditoria). Sin dependencias de RabbitMQ. */
@Service
public class AuditoriaService {

    public record RegistroAuditoria(String mensajeId, String pedidoId, String estado, String routingKey, Instant registradoEn) {
    }

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final int MAX_HISTORIAL = 200;

    private final Deque<RegistroAuditoria> registros = new ConcurrentLinkedDeque<>();

    public RegistroAuditoria registrar(PedidoFinalizadoMensaje msg, String routingKey) {
        if (msg == null || msg.mensajeId() == null || msg.pedidoId() == null || msg.estado() == null) {
            throw new PayloadInvalidoException("Mensaje de auditoría incompleto: " + msg);
        }
        RegistroAuditoria r = new RegistroAuditoria(msg.mensajeId(), msg.pedidoId(), msg.estado(), routingKey, Instant.now());
        registros.addLast(r);
        while (registros.size() > MAX_HISTORIAL) {
            registros.pollFirst();
        }
        log.info("[AUDITORIA] pedido={} estado={} routingKey={}", msg.pedidoId(), msg.estado(), routingKey);
        return r;
    }

    public List<RegistroAuditoria> listar() {
        List<RegistroAuditoria> copia = new ArrayList<>(registros);
        Collections.reverse(copia);
        return copia;
    }
}
