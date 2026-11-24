package cl.duoc.streaming.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "nombre inválido") String name,
        @NotBlank @Pattern(regexp = "direct|topic|fanout|headers", message = "tipo: direct, topic, fanout o headers") String type,
        Boolean durable) {

    public ExchangeRequest {
        if (durable == null) {
            durable = true;
        }
    }
}
