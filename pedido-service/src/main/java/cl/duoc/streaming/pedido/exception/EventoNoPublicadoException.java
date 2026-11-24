package cl.duoc.streaming.pedido.exception;

public class EventoNoPublicadoException extends RuntimeException {
    public EventoNoPublicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
