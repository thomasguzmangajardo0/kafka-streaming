package cl.duoc.streaming.pedido.controller;

import cl.duoc.streaming.pedido.dto.CrearPedidoRequest;
import cl.duoc.streaming.pedido.dto.PedidoResponse;
import cl.duoc.streaming.pedido.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED) // 202: el resultado final llega de forma asíncrona vía eventos
    public PedidoResponse crear(@Valid @RequestBody CrearPedidoRequest request) {
        return PedidoResponse.from(service.crear(request));
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable String id) {
        return PedidoResponse.from(service.obtener(id));
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return service.listar().stream().map(PedidoResponse::from).toList();
    }
}
