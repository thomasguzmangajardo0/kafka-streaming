package cl.duoc.streaming.kafkaadmin.service;

import cl.duoc.streaming.kafkaadmin.dto.ClusterInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static cl.duoc.streaming.kafkaadmin.exception.KafkaFutures.await;

/** Estado del clúster: id, controlador y brokers vivos (útil para demostrar la tolerancia a fallos). */
@Service
public class ClusterAdminService {

    private final AdminClient admin;

    public ClusterAdminService(AdminClient admin) {
        this.admin = admin;
    }

    public ClusterInfo describir() {
        DescribeClusterResult r = admin.describeCluster();
        String clusterId = await(r.clusterId());
        Node controlador = await(r.controller());
        List<ClusterInfo.Broker> brokers = await(r.nodes()).stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new ClusterInfo.Broker(n.id(), n.host(), n.port(), n.rack()))
                .toList();
        return new ClusterInfo(clusterId, controlador != null ? controlador.id() : -1, brokers);
    }
}
