package cl.duoc.streaming.notificacion.domain;

import java.time.Instant;

/** Notificación generada a partir de un evento (simula el envío de un correo). */
public record Notificacion(
        String id,
        String pedidoId,
        String destinatario,
        String tipo,
        String asunto,
        String mensaje,
        Instant creadaEn) {
}
