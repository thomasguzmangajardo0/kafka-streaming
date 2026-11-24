package cl.duoc.streaming.kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Configuraciones a modificar en un tópico, p. ej. {"retention.ms": "86400000"}. */
public record ConfigUpdateRequest(@NotEmpty Map<String, String> configs) {
}
