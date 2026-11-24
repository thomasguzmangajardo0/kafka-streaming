package cl.duoc.streaming.rabbitadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param conDlq               si es true crea además la cola {@code <name>.dlq} y la deja como destino de mensajes muertos
 * @param deadLetterExchange   alternativa manual a {@code conDlq}
 * @param deadLetterRoutingKey alternativa manual a {@code conDlq}
 * @param messageTtlMs         tiempo de vida de los mensajes (opcional)
 */
public record QueueRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "nombre inválido") String name,
        Boolean durable,
        Boolean conDlq,
        String deadLetterExchange,
        String deadLetterRoutingKey,
        @Min(1) Long messageTtlMs) {

    public QueueRequest {
        if (durable == null) {
            durable = true;
        }
        if (conDlq == null) {
            conDlq = false;
        }
    }
}
