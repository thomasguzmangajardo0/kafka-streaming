package cl.duoc.streaming.inventario.service;

import cl.duoc.streaming.inventario.config.InventarioProperties;
import cl.duoc.streaming.inventario.dto.EstadoStock;
import cl.duoc.streaming.inventario.dto.ItemPedido;
import cl.duoc.streaming.inventario.dto.PedidoEvento;
import cl.duoc.streaming.inventario.dto.StockEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica de reserva de stock. No conoce Kafka: recibe un {@link PedidoEvento} y entrega
 * el resultado mediante el puerto {@link StockEventPublisher}.
 */
@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final Map<String, Integer> stock = new HashMap<>(); // protegido por "this"
    private final Set<String> eventosProcesados = ConcurrentHashMap.newKeySet();
    private final StockEventPublisher publisher;

    public InventarioService(InventarioProperties props, StockEventPublisher publisher) {
        this.stock.putAll(props.stockInicial());
        this.publisher = publisher;
    }

    /** Procesa un pedido: valida, reserva (o rechaza) y publica el resultado. Idempotente por eventId. */
    public void procesarPedido(PedidoEvento evento) {
        PedidoEventoValidator.validar(evento);

        if (!eventosProcesados.add(evento.eventId())) {
            log.warn("Evento {} ya procesado, se ignora (entrega duplicada)", evento.eventId());
            return;
        }

        StockEvento resultado = reservar(evento);
        try {
            publisher.publicar(resultado);
        } catch (RuntimeException e) {
            // Si no se pudo informar el resultado, se deshace la reserva y se permite reintentar
            if (resultado.estado() == EstadoStock.RESERVADO) {
                liberar(evento);
            }
            eventosProcesados.remove(evento.eventId());
            throw e;
        }
    }

    private synchronized StockEvento reservar(PedidoEvento evento) {
        for (ItemPedido item : evento.items()) {
            Integer disponible = stock.get(item.sku());
            if (disponible == null) {
                return resultado(evento, EstadoStock.RECHAZADO, "SKU inexistente: " + item.sku());
            }
            if (disponible < item.cantidad()) {
                return resultado(evento, EstadoStock.RECHAZADO,
                        "Stock insuficiente para " + item.sku() + " (disponible " + disponible
                                + ", solicitado " + item.cantidad() + ")");
            }
        }
        evento.items().forEach(i -> stock.merge(i.sku(), -i.cantidad(), Integer::sum));
        log.info("Stock reservado para pedido {}", evento.pedidoId());
        return resultado(evento, EstadoStock.RESERVADO, null);
    }

    private synchronized void liberar(PedidoEvento evento) {
        evento.items().forEach(i -> stock.merge(i.sku(), i.cantidad(), Integer::sum));
        log.warn("Reserva revertida para pedido {} (no se pudo publicar el resultado)", evento.pedidoId());
    }

    public synchronized Map<String, Integer> consultarStock() {
        return Map.copyOf(stock);
    }

    private StockEvento resultado(PedidoEvento evento, EstadoStock estado, String motivo) {
        String tipo = estado == EstadoStock.RESERVADO ? "STOCK_RESERVADO" : "STOCK_RECHAZADO";
        if (estado == EstadoStock.RECHAZADO) {
            log.info("Pedido {} rechazado: {}", evento.pedidoId(), motivo);
        }
        return new StockEvento(UUID.randomUUID().toString(), tipo, evento.pedidoId(), evento.clienteEmail(),
                estado, motivo, evento.items(), Instant.now());
    }
}
