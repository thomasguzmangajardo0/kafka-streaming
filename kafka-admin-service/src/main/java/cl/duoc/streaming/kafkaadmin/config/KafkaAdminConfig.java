package cl.duoc.streaming.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente administrativo de Kafka:
 * - {@link KafkaAdmin} crea / corrige al arrancar los tópicos declarados en application.yml.
 * - {@link AdminClient} es el que usan los servicios REST de administración.
 */
@Configuration
@EnableConfigurationProperties(AdminKafkaProperties.class)
public class KafkaAdminConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(AdminKafkaProperties props) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        cfg.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 15_000);
        cfg.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 20_000);
        KafkaAdmin admin = new KafkaAdmin(cfg);
        admin.setAutoCreate(true);
        admin.setFatalIfBrokerNotAvailable(false); // el servicio arranca aunque Kafka aún no esté listo
        admin.setModifyTopicConfigs(true);          // si cambian retención / cleanup en el yml, se aplican
        return admin;
    }

    /** Tópicos del sistema declarados en application.yml; KafkaAdmin los crea al iniciar. */
    @Bean
    public KafkaAdmin.NewTopics topicosDelSistema(AdminKafkaProperties props) {
        NewTopic[] topics = props.topics().stream()
                .map(TopicDefinition::toNewTopic)
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
