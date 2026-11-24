package cl.duoc.streaming.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Nuevo número TOTAL de particiones (Kafka solo permite aumentar). */
public record PartitionsRequest(@Min(1) @Max(100) int total) {
}
