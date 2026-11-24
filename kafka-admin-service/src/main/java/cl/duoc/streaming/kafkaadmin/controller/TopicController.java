package cl.duoc.streaming.kafkaadmin.controller;

import cl.duoc.streaming.kafkaadmin.dto.ConfigUpdateRequest;
import cl.duoc.streaming.kafkaadmin.dto.PartitionsRequest;
import cl.duoc.streaming.kafkaadmin.dto.TopicDetalle;
import cl.duoc.streaming.kafkaadmin.dto.TopicRequest;
import cl.duoc.streaming.kafkaadmin.dto.TopicResumen;
import cl.duoc.streaming.kafkaadmin.service.TopicAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kafka/topics")
public class TopicController {

    private final TopicAdminService service;

    public TopicController(TopicAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicRequest crear(@Valid @RequestBody TopicRequest request) {
        service.crear(request);
        return request;
    }

    @GetMapping
    public List<TopicResumen> listar() {
        return service.listar();
    }

    @GetMapping("/{name}")
    public TopicDetalle describir(@PathVariable String name) {
        return service.describir(name);
    }

    /** Modifica configuraciones (retention.ms, cleanup.policy, ...). */
    @PatchMapping("/{name}/config")
    public TopicDetalle actualizarConfig(@PathVariable String name, @Valid @RequestBody ConfigUpdateRequest request) {
        service.actualizarConfiguracion(name, request.configs());
        return service.describir(name);
    }

    /** Aumenta el número total de particiones. */
    @PutMapping("/{name}/partitions")
    public TopicDetalle aumentarParticiones(@PathVariable String name, @Valid @RequestBody PartitionsRequest request) {
        service.aumentarParticiones(name, request.total());
        return service.describir(name);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String name) {
        service.eliminar(name);
    }
}
