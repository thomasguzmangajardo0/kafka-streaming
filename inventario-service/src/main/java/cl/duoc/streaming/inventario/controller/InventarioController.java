package cl.duoc.streaming.inventario.controller;

import cl.duoc.streaming.inventario.service.DespachoService;
import cl.duoc.streaming.inventario.service.InventarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService service;
    private final DespachoService despachoService;

    public InventarioController(InventarioService service, DespachoService despachoService) {
        this.service = service;
        this.despachoService = despachoService;
    }

    /** Stock actual por SKU: sirve para mostrar en la demo cómo baja al llegar nuevos pedidos. */
    @GetMapping
    public Map<String, Integer> stock() {
        return new TreeMap<>(service.consultarStock());
    }

    /** Despachos programados a partir de los mensajes RabbitMQ de pedidos confirmados. */
    @GetMapping("/despachos")
    public List<DespachoService.Despacho> despachos() {
        return despachoService.listar();
    }
}
