package cl.duoc.streaming.kafkaadmin.controller;

import cl.duoc.streaming.kafkaadmin.dto.ClusterInfo;
import cl.duoc.streaming.kafkaadmin.service.ClusterAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kafka/cluster")
public class ClusterController {

    private final ClusterAdminService service;

    public ClusterController(ClusterAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ClusterInfo describir() {
        return service.describir();
    }
}
