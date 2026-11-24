package cl.duoc.streaming.pedido.service;

import cl.duoc.streaming.pedido.dto.PedidoEvento;

/** Puerto de salida: la lógica de negocio no conoce Kafka, solo esta interfaz. */
public interface PedidoEventPublisher {

    /** Publica el evento y lanza EventoNoPublicadoException si el broker no lo confirma. */
    void publicar(PedidoEvento evento);
}
