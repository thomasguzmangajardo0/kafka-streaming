package cl.duoc.streaming.inventario.exception;

public class EventoNoPublicadoException extends RuntimeException {
    public EventoNoPublicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
