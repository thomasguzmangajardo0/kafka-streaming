package cl.duoc.streaming.kafkaadmin.controller;

import cl.duoc.streaming.kafkaadmin.dto.GrupoDetalle;
import cl.duoc.streaming.kafkaadmin.dto.GrupoResumen;
import cl.duoc.streaming.kafkaadmin.dto.LagGrupo;
import cl.duoc.streaming.kafkaadmin.dto.ResetOffsetsRequest;
import cl.duoc.streaming.kafkaadmin.service.ConsumerGroupAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kafka/consumer-groups")
public class ConsumerGroupController {

    private final ConsumerGroupAdminService service;

    public ConsumerGroupController(ConsumerGroupAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<GrupoResumen> listar() {
        return service.listar();
    }

    @GetMapping("/{groupId}")
    public GrupoDetalle describir(@PathVariable String groupId) {
        return service.describir(groupId);
    }

    @GetMapping("/{groupId}/lag")
    public LagGrupo lag(@PathVariable String groupId) {
        return service.lag(groupId);
    }

    @PostMapping("/{groupId}/reset-offsets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reiniciarOffsets(@PathVariable String groupId, @Valid @RequestBody ResetOffsetsRequest request) {
        service.reiniciarOffsets(groupId, request.topic(), request.to());
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String groupId) {
        service.eliminar(groupId);
    }
}
