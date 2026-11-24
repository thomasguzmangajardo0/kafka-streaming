package cl.duoc.streaming.notificacion.exception;

/** El evento recibido no cumple el contrato (campos faltantes, valores inválidos). No se reintenta. */
public class PayloadInvalidoException extends RuntimeException {
    public PayloadInvalidoException(String message) {
        super(message);
    }
}
