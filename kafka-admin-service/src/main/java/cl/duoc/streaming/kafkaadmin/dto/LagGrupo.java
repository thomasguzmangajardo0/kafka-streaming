package cl.duoc.streaming.kafkaadmin.dto;

import java.util.List;

/** Lag de un consumer group: diferencia entre el último offset del log y el offset confirmado. */
public record LagGrupo(String groupId, long lagTotal, List<LagParticion> partitions) {

    public record LagParticion(String topic, int partition, long offsetActual, long logEndOffset, long lag) {
    }
}
