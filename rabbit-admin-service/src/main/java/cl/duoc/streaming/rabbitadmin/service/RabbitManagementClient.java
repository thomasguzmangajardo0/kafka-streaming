package cl.duoc.streaming.rabbitadmin.service;

import cl.duoc.streaming.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.streaming.rabbitadmin.dto.PolicyRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta y configura RabbitMQ mediante su API HTTP de administración (la misma que usa la consola web).
 * Se usa para lo que AMQP no ofrece: listar recursos, ver el clúster y aplicar políticas.
 */
@Component
public class RabbitManagementClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() { };

    private final RestClient rest;
    private final String base;
    private final String vhost;

    public RabbitManagementClient(RestClient rabbitManagementRestClient, RabbitAdminProperties props) {
        this.rest = rabbitManagementRestClient;
        this.base = props.management().url().replaceAll("/+$", "");
        this.vhost = URLEncoder.encode(props.vhost(), StandardCharsets.UTF_8); // "/" => %2F
    }

    /** Nodos del clúster: nombre, si están activos, memoria y tiempo encendido. */
    public Map<String, Object> cluster() {
        List<Map<String, Object>> nodos = obtener("/api/nodes").stream()
                .map(n -> seleccionar(n, "name", "running", "type", "mem_used", "mem_limit", "fd_used", "uptime", "partitions"))
                .toList();
        long activos = nodos.stream().filter(n -> Boolean.TRUE.equals(n.get("running"))).count();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalNodos", nodos.size());
        resumen.put("nodosActivos", activos);
        resumen.put("enCluster", nodos.size() > 1);
        resumen.put("nodos", nodos);
        return resumen;
    }

    /** Colas con su estado: mensajes, consumidores, nodo líder y réplicas (por política HA). */
    public List<Map<String, Object>> colas() {
        return obtener("/api/queues/" + vhost).stream()
                .map(q -> seleccionar(q, "name", "state", "durable", "messages", "messages_ready",
                        "messages_unacknowledged", "consumers", "node", "slave_nodes", "policy", "arguments"))
                .toList();
    }

    public List<Map<String, Object>> exchanges() {
        return obtener("/api/exchanges/" + vhost).stream()
                .filter(e -> !String.valueOf(e.get("name")).isEmpty() && !String.valueOf(e.get("name")).startsWith("amq."))
                .map(e -> seleccionar(e, "name", "type", "durable", "internal"))
                .toList();
    }

    public List<Map<String, Object>> bindings() {
        return obtener("/api/bindings/" + vhost).stream()
                .filter(b -> !String.valueOf(b.get("source")).isEmpty())
                .map(b -> seleccionar(b, "source", "destination", "destination_type", "routing_key"))
                .toList();
    }

    public List<Map<String, Object>> politicas() {
        return obtener("/api/policies/" + vhost);
    }

    public void aplicarPolitica(String nombre, PolicyRequest p) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pattern", p.pattern());
        cuerpo.put("definition", p.definition());
        cuerpo.put("priority", p.priority());
        cuerpo.put("apply-to", "queues");
        rest.put().uri(URI.create(base + "/api/policies/" + vhost + "/" + URLEncoder.encode(nombre, StandardCharsets.UTF_8)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    private List<Map<String, Object>> obtener(String ruta) {
        List<Map<String, Object>> r = rest.get().uri(URI.create(base + ruta)).retrieve().body(LISTA);
        return r == null ? List.of() : r;
    }

    private static Map<String, Object> seleccionar(Map<String, Object> origen, String... claves) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String c : claves) {
            if (origen.containsKey(c)) {
                m.put(c, origen.get(c));
            }
        }
        return m;
    }
}
