package cl.duoc.streaming.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /api/kafka/topics. Los campos opcionales usan valores por defecto razonables. */
public record TopicRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "nombre de tópico inválido") String name,
        @Min(1) @Max(100) int partitions,
        @Min(1) @Max(10) short replicas,
        Long retentionMs,
        String cleanupPolicy,
        Integer minInsyncReplicas) {

    public static final long RETENCION_POR_DEFECTO_MS = 604_800_000L; // 7 días

    public long retentionMsOrDefault() {
        return retentionMs != null ? retentionMs : RETENCION_POR_DEFECTO_MS;
    }

    public String cleanupPolicyOrDefault() {
        return cleanupPolicy != null && !cleanupPolicy.isBlank() ? cleanupPolicy : "delete";
    }
}
