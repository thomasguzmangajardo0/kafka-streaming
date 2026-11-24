package cl.duoc.streaming.kafkaadmin.exception;

import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores de Kafka a respuestas HTTP con sentido (404, 409, 400, 502). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(KafkaOperacionException.class)
    public ProblemDetail errorKafka(KafkaOperacionException ex) {
        Throwable causa = ex.getCause();
        HttpStatus status;
        if (causa instanceof UnknownTopicOrPartitionException || causa instanceof GroupIdNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (causa instanceof TopicExistsException || causa instanceof GroupNotEmptyException) {
            status = HttpStatus.CONFLICT;
        } else if (causa instanceof InvalidTopicException || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidPartitionsException || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidRequestException) {
            status = HttpStatus.BAD_REQUEST;
        } else {
            log.error("Error inesperado hablando con Kafka", ex);
            status = HttpStatus.BAD_GATEWAY;
        }
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }
}
