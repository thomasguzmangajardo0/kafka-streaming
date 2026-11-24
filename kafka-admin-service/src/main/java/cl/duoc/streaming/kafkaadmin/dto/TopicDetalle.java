package cl.duoc.streaming.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

/** Detalle de un tópico: distribución de particiones (líder, réplicas, ISR) y configuraciones explícitas. */
public record TopicDetalle(
        String name,
        boolean internal,
        List<Particion> partitions,
        Map<String, String> configs) {

    public record Particion(int id, int leader, List<Integer> replicas, List<Integer> isr) {
    }
}
