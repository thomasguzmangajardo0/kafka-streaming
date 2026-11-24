package cl.duoc.streaming.inventario.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;

/** Registra en el log cada mensaje que termina en la DLT y luego delega la publicación. */
public class DeadLetterLogger implements ConsumerRecordRecoverer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterLogger.class);

    private final ConsumerRecordRecoverer delegate;

    public DeadLetterLogger(ConsumerRecordRecoverer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(exception);
        log.error("[DLQ] Mensaje descartado -> {}.DLT | topic={} partition={} offset={} key={} causa={}: {}",
                record.topic(), record.topic(), record.partition(), record.offset(), record.key(),
                causa.getClass().getSimpleName(), causa.getMessage());
        delegate.accept(record, exception);
    }
}
