package cl.duoc.streaming.rabbitadmin.service;

import cl.duoc.streaming.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.streaming.rabbitadmin.dto.BindingRequest;
import cl.duoc.streaming.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.streaming.rabbitadmin.dto.QueueRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TopologiaServiceTest {

    private AmqpAdmin amqpAdmin;
    private TopologiaService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        RabbitAdminProperties props = new RabbitAdminProperties(
                new RabbitAdminProperties.Management("http://localhost:15672", "admin", "admin123"),
                "/", true, null, "dlx.direct",
                List.of(new RabbitAdminProperties.ExchangeDef("pedidos.direct", "direct"),
                        new RabbitAdminProperties.ExchangeDef("eventos.topic", "topic")),
                List.of(new RabbitAdminProperties.QueueDef("cola.a", true),
                        new RabbitAdminProperties.QueueDef("cola.b", true),
                        new RabbitAdminProperties.QueueDef("cola.c", true)),
                List.of(new RabbitAdminProperties.BindingDef("pedidos.direct", "cola.a", "pedido.confirmado")));
        service = new TopologiaService(amqpAdmin, props);
    }

    @Test
    void crearColaConDlqDeclaraDlqBindingYColaPrincipalConArgumentos() {
        service.declararCola(new QueueRequest("cola.demo", true, true, null, null, null));

        ArgumentCaptor<Queue> colas = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin, times(2)).declareQueue(colas.capture());
        Queue dlq = colas.getAllValues().get(0);
        Queue principal = colas.getAllValues().get(1);
        assertEquals("cola.demo.dlq", dlq.getName());
        assertEquals("cola.demo", principal.getName());
        assertEquals("dlx.direct", principal.getArguments().get("x-dead-letter-exchange"));
        assertEquals("cola.demo.dlq", principal.getArguments().get("x-dead-letter-routing-key"));

        ArgumentCaptor<Binding> binding = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(binding.capture());
        assertEquals("cola.demo.dlq", binding.getValue().getDestination());
        assertEquals("dlx.direct", binding.getValue().getExchange());
    }

    @Test
    void crearColaSimpleConTtlYDlqManual() {
        service.declararCola(new QueueRequest("cola.simple", true, false, "dlx.direct", "x.dlq", 60000L));

        ArgumentCaptor<Queue> cola = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(cola.capture());
        assertEquals(60000L, cola.getValue().getArguments().get("x-message-ttl"));
        assertEquals("x.dlq", cola.getValue().getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void creaExchangesDeCadaTipo() {
        service.declararExchange(new ExchangeRequest("ex.d", "direct", true));
        service.declararExchange(new ExchangeRequest("ex.t", "topic", true));

        ArgumentCaptor<Exchange> ex = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin, times(2)).declareExchange(ex.capture());
        assertInstanceOf(DirectExchange.class, ex.getAllValues().get(0));
        assertInstanceOf(TopicExchange.class, ex.getAllValues().get(1));
    }

    @Test
    void enlazaYDesenlazaConLaRoutingKey() {
        BindingRequest b = new BindingRequest("eventos.topic", "cola.notificaciones", "pedido.*");
        service.enlazar(b);
        service.desenlazar(b);

        ArgumentCaptor<Binding> declarado = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(declarado.capture());
        assertEquals("pedido.*", declarado.getValue().getRoutingKey());
        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarReportaSiExistia() {
        when(amqpAdmin.deleteQueue("cola.x")).thenReturn(true);
        when(amqpAdmin.deleteQueue("cola.y")).thenReturn(false);
        assertTrue(service.eliminarCola("cola.x"));
        assertFalse(service.eliminarCola("cola.y"));
    }

    @Test
    void noPermiteAdministrarRecursosInternos() {
        assertThrows(IllegalArgumentException.class, () -> service.eliminarExchange("amq.direct"));
        assertThrows(IllegalArgumentException.class, () -> service.eliminarCola(""));
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void aplicarTopologiaConfiguradaCreaTresColasConDlqExchangesYBindings() {
        int operaciones = service.aplicarTopologiaConfigurada();

        assertEquals(2 + 3 + 1, operaciones);
        // 3 colas + 3 DLQ
        verify(amqpAdmin, times(6)).declareQueue(any(Queue.class));
        // 2 exchanges configurados + el DLX declarado por cada cola con DLQ
        verify(amqpAdmin, times(2 + 3)).declareExchange(any(Exchange.class));
        // 3 bindings de DLQ + 1 binding configurado
        verify(amqpAdmin, times(4)).declareBinding(any(Binding.class));
    }

    @Test
    void conservaLosArgumentosComoMapa() {
        service.declararCola(new QueueRequest("cola.demo", true, true, null, null, 1000L));
        ArgumentCaptor<Queue> colas = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin, times(2)).declareQueue(colas.capture());
        Map<String, Object> args = colas.getAllValues().get(1).getArguments();
        assertEquals(1000L, args.get("x-message-ttl"));
    }
}
