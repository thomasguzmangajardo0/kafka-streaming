package cl.duoc.streaming.inventario.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

import java.nio.charset.StandardCharsets;

/**
 * Se ejecuta cuando un mensaje agotó sus reintentos. Deja registro en el log con el detalle
 * necesario para investigarlo y lo rechaza sin reencolar: la cola de origen tiene configurado
 * {@code x-dead-letter-exchange}, por lo que RabbitMQ lo deriva a su DLQ ({@code <cola>.dlq}).
 */
public class DlqLoggingRecoverer implements MessageRecoverer {

    private static final Logger log = LoggerFactory.getLogger(DlqLoggingRecoverer.class);
    private static final int MAX_CUERPO = 500;

    @Override
    public void recover(Message message, Throwable cause) {
        MessageProperties p = message.getMessageProperties();
        String cuerpo = new String(message.getBody(), StandardCharsets.UTF_8);
        if (cuerpo.length() > MAX_CUERPO) {
            cuerpo = cuerpo.substring(0, MAX_CUERPO) + "...";
        }
        log.error("[DLQ] Mensaje enviado a la DLQ tras agotar reintentos: cola={} exchange={} routingKey={} messageId={} causa={} cuerpo={}",
                p.getConsumerQueue(), p.getReceivedExchange(), p.getReceivedRoutingKey(), p.getMessageId(),
                causaRaiz(cause), cuerpo);
        throw new ListenerExecutionFailedException("Reintentos agotados para " + p.getConsumerQueue(),
                new AmqpRejectAndDontRequeueException(cause), message);
    }

    private static String causaRaiz(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) {
            r = r.getCause();
        }
        return r.getClass().getSimpleName() + ": " + r.getMessage();
    }
}
