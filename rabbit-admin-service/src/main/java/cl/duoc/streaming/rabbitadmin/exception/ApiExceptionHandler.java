package cl.duoc.streaming.rabbitadmin.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ProblemDetail sinConexion(AmqpConnectException ex) {
        log.error("RabbitMQ no disponible", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "No hay conexión con RabbitMQ");
    }

    /** Típico: volver a declarar una cola existente con argumentos distintos (PRECONDITION_FAILED). */
    @ExceptionHandler(AmqpException.class)
    public ProblemDetail errorAmqp(AmqpException ex) {
        Throwable raiz = ex;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        log.warn("Operación AMQP rechazada: {}", raiz.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "RabbitMQ rechazó la operación: " + raiz.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ProblemDetail noEncontrado(HttpClientErrorException.NotFound ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso no encontrado en RabbitMQ");
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail errorApiAdministracion(RestClientException ex) {
        log.error("Falla al consultar la API de administración de RabbitMQ", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "No se pudo consultar la API de administración de RabbitMQ");
    }
}
