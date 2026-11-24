package cl.duoc.streaming.inventario.service;

import cl.duoc.streaming.inventario.dto.StockEvento;

/** Puerto de salida: la lógica de inventario no depende de Kafka. */
public interface StockEventPublisher {

    /** Publica el resultado de stock; lanza excepción si el broker no lo confirma. */
    void publicar(StockEvento evento);
}
