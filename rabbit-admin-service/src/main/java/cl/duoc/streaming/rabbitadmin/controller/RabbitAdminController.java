package cl.duoc.streaming.rabbitadmin.controller;

import cl.duoc.streaming.rabbitadmin.dto.BindingRequest;
import cl.duoc.streaming.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.streaming.rabbitadmin.dto.PolicyRequest;
import cl.duoc.streaming.rabbitadmin.dto.QueueRequest;
import cl.duoc.streaming.rabbitadmin.service.RabbitManagementClient;
import cl.duoc.streaming.rabbitadmin.service.TopologiaInicial;
import cl.duoc.streaming.rabbitadmin.service.TopologiaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** API REST de administración de RabbitMQ: crear/eliminar colas, exchanges y bindings; inspeccionar el clúster. */
@RestController
@RequestMapping("/api/rabbit")
public class RabbitAdminController {

    private final TopologiaService topologia;
    private final RabbitManagementClient management;
    private final TopologiaInicial topologiaInicial;

    public RabbitAdminController(TopologiaService topologia, RabbitManagementClient management,
                                 TopologiaInicial topologiaInicial) {
        this.topologia = topologia;
        this.management = management;
        this.topologiaInicial = topologiaInicial;
    }

    // ------------------------------------------------------------ inspección
    @GetMapping("/cluster")
    public Map<String, Object> cluster() {
        return management.cluster();
    }

    @GetMapping("/queues")
    public List<Map<String, Object>> colas() {
        return management.colas();
    }

    @GetMapping("/exchanges")
    public List<Map<String, Object>> exchanges() {
        return management.exchanges();
    }

    @GetMapping("/bindings")
    public List<Map<String, Object>> bindings() {
        return management.bindings();
    }

    @GetMapping("/policies")
    public List<Map<String, Object>> politicas() {
        return management.politicas();
    }

    // ----------------------------------------------------------------- colas
    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public void crearCola(@Valid @RequestBody QueueRequest request) {
        topologia.declararCola(request);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        return topologia.eliminarCola(name) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ------------------------------------------------------------- exchanges
    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public void crearExchange(@Valid @RequestBody ExchangeRequest request) {
        topologia.declararExchange(request);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        return topologia.eliminarExchange(name) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // -------------------------------------------------------------- bindings
    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public void crearBinding(@Valid @RequestBody BindingRequest request) {
        topologia.enlazar(request);
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBinding(@Valid @RequestBody BindingRequest request) {
        topologia.desenlazar(request);
    }

    // -------------------------------------------------------------- políticas
    @PutMapping("/policies/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aplicarPolitica(@PathVariable String name, @Valid @RequestBody PolicyRequest request) {
        management.aplicarPolitica(name, request);
    }

    // --------------------------------------------------------------- topología
    /** Vuelve a aplicar la topología de application.yml (operación replicable e idempotente). */
    @PostMapping("/topologia/aplicar")
    public Map<String, Object> aplicarTopologia() {
        return Map.of("operaciones", topologiaInicial.aplicar());
    }
}
