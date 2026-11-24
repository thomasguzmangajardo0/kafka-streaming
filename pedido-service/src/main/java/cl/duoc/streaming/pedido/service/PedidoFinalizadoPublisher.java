package cl.duoc.streaming.pedido.service;

import cl.duoc.streaming.pedido.dto.PedidoFinalizadoMensaje;

/** Puerto de salida hacia RabbitMQ: avisa que un pedido terminó (confirmado o rechazado). */
public interface PedidoFinalizadoPublisher {

    /** Lanza EventoNoPublicadoException si el broker no acepta el mensaje. */
    void publicar(PedidoFinalizadoMensaje mensaje);
}
