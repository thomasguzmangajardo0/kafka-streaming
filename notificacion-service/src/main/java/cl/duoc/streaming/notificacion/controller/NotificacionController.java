package cl.duoc.streaming.notificacion.controller;

import cl.duoc.streaming.notificacion.domain.Notificacion;
import cl.duoc.streaming.notificacion.service.AuditoriaService;
import cl.duoc.streaming.notificacion.service.NotificacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;
    private final AuditoriaService auditoriaService;

    public NotificacionController(NotificacionService service, AuditoriaService auditoriaService) {
        this.service = service;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<Notificacion> listar() {
        return service.listar();
    }

    /** Registros de auditoría recibidos por RabbitMQ (cola.auditoria). */
    @GetMapping("/auditoria")
    public List<AuditoriaService.RegistroAuditoria> auditoria() {
        return auditoriaService.listar();
    }
}
