package cl.duoc.streaming.rabbitadmin.service;

import cl.duoc.streaming.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.streaming.rabbitadmin.dto.BindingRequest;
import cl.duoc.streaming.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.streaming.rabbitadmin.dto.QueueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Crea y elimina colas, exchanges y bindings mediante {@link AmqpAdmin}.
 * Todas las declaraciones son idempotentes: repetirlas con los mismos argumentos no cambia nada.
 */
@Service
public class TopologiaService {

    private static final Logger log = LoggerFactory.getLogger(TopologiaService.class);
    static final String SUFIJO_DLQ = ".dlq";

    private final AmqpAdmin amqpAdmin;
    private final RabbitAdminProperties props;

    public TopologiaService(AmqpAdmin amqpAdmin, RabbitAdminProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.props = props;
    }

    // ---------------------------------------------------------------- exchanges
    public void declararExchange(ExchangeRequest r) {
        validarNombreEditable(r.name());
        Exchange exchange = switch (r.type()) {
            case "direct" -> new DirectExchange(r.name(), r.durable(), false);
            case "topic" -> new TopicExchange(r.name(), r.durable(), false);
            case "fanout" -> new FanoutExchange(r.name(), r.durable(), false);
            case "headers" -> new HeadersExchange(r.name(), r.durable(), false);
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + r.type());
        };
        amqpAdmin.declareExchange(exchange);
        log.info("Exchange declarado: {} ({})", r.name(), r.type());
    }

    /** @return true si existía y fue eliminado */
    public boolean eliminarExchange(String nombre) {
        validarNombreEditable(nombre);
        boolean eliminado = amqpAdmin.deleteExchange(nombre);
        log.info("Exchange {} eliminado={}", nombre, eliminado);
        return eliminado;
    }

    // ------------------------------------------------------------------- colas
    public void declararCola(QueueRequest r) {
        validarNombreEditable(r.name());
        if (r.conDlq()) {
            declararColaConDlq(r.name(), r.durable(), r.messageTtlMs());
            return;
        }
        Map<String, Object> args = new HashMap<>();
        if (StringUtils.hasText(r.deadLetterExchange())) {
            args.put("x-dead-letter-exchange", r.deadLetterExchange());
        }
        if (StringUtils.hasText(r.deadLetterRoutingKey())) {
            args.put("x-dead-letter-routing-key", r.deadLetterRoutingKey());
        }
        if (r.messageTtlMs() != null) {
            args.put("x-message-ttl", r.messageTtlMs());
        }
        amqpAdmin.declareQueue(new Queue(r.name(), r.durable(), false, false, args));
        log.info("Cola declarada: {} args={}", r.name(), args);
    }

    /**
     * Crea la cola y su DLQ: {@code <nombre>.dlq} enlazada al exchange de mensajes muertos con la clave
     * {@code <nombre>.dlq}; la cola principal apunta a ese exchange/clave en sus argumentos.
     */
    public void declararColaConDlq(String nombre, boolean durable, Long ttlMs) {
        String dlx = props.deadLetterExchange();
        String dlq = nombre + SUFIJO_DLQ;

        amqpAdmin.declareExchange(new DirectExchange(dlx, true, false));
        amqpAdmin.declareQueue(new Queue(dlq, durable));
        amqpAdmin.declareBinding(new Binding(dlq, Binding.DestinationType.QUEUE, dlx, dlq, null));

        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlx);
        args.put("x-dead-letter-routing-key", dlq);
        if (ttlMs != null) {
            args.put("x-message-ttl", ttlMs);
        }
        amqpAdmin.declareQueue(new Queue(nombre, durable, false, false, args));
        log.info("Cola {} declarada con su DLQ {}", nombre, dlq);
    }

    /** @return true si existía y fue eliminada */
    public boolean eliminarCola(String nombre) {
        validarNombreEditable(nombre);
        boolean eliminada = amqpAdmin.deleteQueue(nombre);
        log.info("Cola {} eliminada={}", nombre, eliminada);
        return eliminada;
    }

    // ---------------------------------------------------------------- bindings
    public void enlazar(BindingRequest r) {
        amqpAdmin.declareBinding(aBinding(r));
        log.info("Binding: {} -[{}]-> {}", r.exchange(), r.routingKey(), r.queue());
    }

    public void desenlazar(BindingRequest r) {
        amqpAdmin.removeBinding(aBinding(r));
        log.info("Binding eliminado: {} -[{}]-> {}", r.exchange(), r.routingKey(), r.queue());
    }

    // --------------------------------------------------------------- topología
    /** Aplica la topología declarada en application.yml (crear o corregir; seguro de repetir). */
    public int aplicarTopologiaConfigurada() {
        int operaciones = 0;
        for (RabbitAdminProperties.ExchangeDef e : props.exchanges()) {
            declararExchange(new ExchangeRequest(e.name(), e.type(), true));
            operaciones++;
        }
        for (RabbitAdminProperties.QueueDef q : props.queues()) {
            if (q.deadLetter()) {
                declararColaConDlq(q.name(), true, null);
            } else {
                amqpAdmin.declareQueue(new Queue(q.name(), true));
            }
            operaciones++;
        }
        for (RabbitAdminProperties.BindingDef b : props.bindings()) {
            enlazar(new BindingRequest(b.exchange(), b.queue(), b.routingKey()));
            operaciones++;
        }
        return operaciones;
    }

    private static Binding aBinding(BindingRequest r) {
        return new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(), r.routingKey(), null);
    }

    /** Evita tocar el exchange por defecto ("") y los internos de RabbitMQ (amq.*). */
    private static void validarNombreEditable(String nombre) {
        if (!StringUtils.hasText(nombre) || nombre.startsWith("amq.")) {
            throw new IllegalArgumentException("No se permite administrar el recurso '" + nombre + "'");
        }
    }
}
