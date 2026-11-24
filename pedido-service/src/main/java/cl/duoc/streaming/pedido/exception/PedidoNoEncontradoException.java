package cl.duoc.streaming.pedido.exception;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(String pedidoId) {
        super("Pedido no encontrado: " + pedidoId);
    }
}
