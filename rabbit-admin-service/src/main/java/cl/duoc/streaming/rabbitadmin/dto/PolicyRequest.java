package cl.duoc.streaming.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Política de RabbitMQ, p.ej. {@code {"pattern":"^cola\\.","definition":{"ha-mode":"all"}}}. */
public record PolicyRequest(@NotBlank String pattern, @NotEmpty Map<String, Object> definition, Integer priority) {

    public PolicyRequest {
        if (priority == null) {
            priority = 0;
        }
    }
}
