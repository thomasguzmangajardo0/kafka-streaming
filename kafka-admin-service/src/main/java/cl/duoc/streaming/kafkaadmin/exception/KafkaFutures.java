package cl.duoc.streaming.kafkaadmin.exception;

import org.apache.kafka.common.KafkaFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Espera el resultado de una operación del AdminClient traduciendo los errores a {@link KafkaOperacionException}. */
public final class KafkaFutures {

    private static final long TIMEOUT_SEGUNDOS = 20;

    private KafkaFutures() {
    }

    public static <T> T await(KafkaFuture<T> future) {
        try {
            return future.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            throw new KafkaOperacionException(causa.getMessage(), causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaOperacionException("Operación interrumpida", e);
        } catch (TimeoutException e) {
            throw new KafkaOperacionException("Tiempo de espera agotado consultando el clúster Kafka", e);
        }
    }
}
