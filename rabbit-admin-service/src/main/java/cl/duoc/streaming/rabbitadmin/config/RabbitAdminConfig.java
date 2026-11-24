package cl.duoc.streaming.rabbitadmin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(RabbitAdminProperties.class)
public class RabbitAdminConfig {

    /** Cliente HTTP hacia la API de administración de RabbitMQ (autenticación básica, con timeouts). */
    @Bean
    public RestClient rabbitManagementRestClient(RabbitAdminProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        RabbitAdminProperties.Management m = props.management();
        String basic = Base64.getEncoder().encodeToString((m.username() + ":" + m.password()).getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Authorization", "Basic " + basic)
                .build();
    }
}
