package cl.duoc.streaming.kafkaadmin.exception;

/** Error al ejecutar una operación administrativa; la causa original de Kafka queda en {@code getCause()}. */
public class KafkaOperacionException extends RuntimeException {
    public KafkaOperacionException(String message, Throwable cause) {
        super(message, cause);
    }
}
