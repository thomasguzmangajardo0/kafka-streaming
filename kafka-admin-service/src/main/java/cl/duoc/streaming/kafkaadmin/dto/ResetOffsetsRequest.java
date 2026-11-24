package cl.duoc.streaming.kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Reubica los offsets de un grupo INACTIVO en un tópico: al inicio ("earliest") o al final ("latest"). */
public record ResetOffsetsRequest(
        @NotBlank String topic,
        @NotBlank @Pattern(regexp = "earliest|latest", message = "debe ser earliest o latest") String to) {
}
