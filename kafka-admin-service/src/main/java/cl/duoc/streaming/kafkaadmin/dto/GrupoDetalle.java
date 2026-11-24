package cl.duoc.streaming.kafkaadmin.dto;

import java.util.List;

/** Estado de un consumer group: coordinador, estrategia de asignación y miembros con sus particiones. */
public record GrupoDetalle(
        String groupId,
        String state,
        String partitionAssignor,
        String coordinador,
        List<Miembro> members) {

    public record Miembro(String consumerId, String clientId, String host, List<String> partitions) {
    }
}
