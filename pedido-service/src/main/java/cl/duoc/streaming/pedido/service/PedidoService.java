package cl.duoc.streaming.pedido.service;

import cl.duoc.streaming.pedido.domain.EstadoPedido;
import cl.duoc.streaming.pedido.domain.Pedido;
import cl.duoc.streaming.pedido.dto.CrearPedidoRequest;
import cl.duoc.streaming.pedido.dto.EstadoStock;
import cl.duoc.streaming.pedido.dto.ItemPedido;
import cl.duoc.streaming.pedido.dto.PedidoEvento;
import cl.duoc.streaming.pedido.dto.PedidoFinalizadoMensaje;
import cl.duoc.streaming.pedido.dto.StockEvento;
import cl.duoc.streaming.pedido.exception.PayloadInvalidoException;
import cl.duoc.streaming.pedido.exception.PedidoNoEncontradoException;
import cl.duoc.streaming.pedido.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Lógica de negocio de pedidos, independiente de la infraestructura de mensajería. */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository repository;
    private final PedidoEventPublisher publisher;
    private final PedidoFinalizadoPublisher finalizadoPublisher;

    public PedidoService(PedidoRepository repository, PedidoEventPublisher publisher,
                         PedidoFinalizadoPublisher finalizadoPublisher) {
        this.repository = repository;
        this.publisher = publisher;
        this.finalizadoPublisher = finalizadoPublisher;
    }

    public Pedido crear(CrearPedidoRequest request) {
        List<ItemPedido> items = request.items().stream()
                .map(i -> new ItemPedido(i.sku(), i.cantidad(), i.precioUnitario()))
                .toList();
        BigDecimal total = items.stream()
                .map(i -> i.precioUnitario().multiply(BigDecimal.valueOf(i.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant ahora = Instant.now();
        Pedido pedido = new Pedido(UUID.randomUUID().toString(), request.clienteId(), request.clienteEmail(),
                items, total, EstadoPedido.PENDIENTE, null, ahora, ahora);
        repository.guardar(pedido);

        PedidoEvento evento = new PedidoEvento(UUID.randomUUID().toString(), PedidoEvento.TIPO_PEDIDO_CREADO,
                pedido.id(), pedido.clienteId(), pedido.clienteEmail(), items, total, ahora);
        try {
            publisher.publicar(evento);
        } catch (RuntimeException e) {
            repository.eliminar(pedido.id()); // no dejamos pedidos "fantasma" sin evento
            throw e;
        }
        return pedido;
    }

    public Pedido obtener(String id) {
        return repository.buscar(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    public Collection<Pedido> listar() {
        return repository.listar();
    }

    /** Reacciona al resultado de stock publicado por inventario y actualiza el estado del pedido. */
    public void aplicarResultadoStock(StockEvento evento) {
        if (evento == null || evento.pedidoId() == null || evento.estado() == null) {
            throw new PayloadInvalidoException("StockEvento incompleto: " + evento);
        }
        Pedido pedido = obtener(evento.pedidoId());
        if (pedido.estado() != EstadoPedido.PENDIENTE) {
            log.warn("Evento duplicado/tardío ignorado para pedido {} (estado actual {})", pedido.id(), pedido.estado());
            return;
        }
        EstadoPedido nuevo = evento.estado() == EstadoStock.RESERVADO ? EstadoPedido.CONFIRMADO : EstadoPedido.RECHAZADO;
        Pedido actualizado = pedido.conEstado(nuevo, evento.motivo());
        // Primero se avisa por RabbitMQ: si falla, el error reintenta el evento de Kafka (y termina en la DLT si persiste)
        finalizadoPublisher.publicar(new PedidoFinalizadoMensaje(UUID.randomUUID().toString(), pedido.id(),
                pedido.clienteEmail(), nuevo.name(), pedido.total(), evento.motivo(), Instant.now()));
        repository.guardar(actualizado);
        log.info("Pedido {} actualizado a {} por evento {}", pedido.id(), nuevo, evento.eventId());
    }
}
