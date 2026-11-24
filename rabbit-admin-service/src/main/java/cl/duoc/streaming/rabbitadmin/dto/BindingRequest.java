package cl.duoc.streaming.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;

public record BindingRequest(@NotBlank String exchange, @NotBlank String queue, String routingKey) {

    public BindingRequest {
        if (routingKey == null) {
            routingKey = "";
        }
    }
}
