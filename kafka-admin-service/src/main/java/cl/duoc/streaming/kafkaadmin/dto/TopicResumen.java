package cl.duoc.streaming.kafkaadmin.dto;

public record TopicResumen(String name, int partitions, int replicationFactor) {
}
