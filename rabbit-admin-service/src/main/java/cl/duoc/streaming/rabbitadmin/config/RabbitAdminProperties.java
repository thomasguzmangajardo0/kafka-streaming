package cl.duoc.streaming.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Map;

/** Topología deseada y datos de acceso a la API de administración (prefijo {@code app.rabbit}). */
@ConfigurationProperties(prefix = "app.rabbit")
public record RabbitAdminProperties(
        Management management,
        @DefaultValue("/") String vhost,
        @DefaultValue("true") boolean applyOnStartup,
        HaPolicy haPolicy,
        @DefaultValue("dlx.direct") String deadLetterExchange,
        @DefaultValue List<ExchangeDef> exchanges,
        @DefaultValue List<QueueDef> queues,
        @DefaultValue List<BindingDef> bindings) {

    public record Management(@DefaultValue("http://localhost:15672") String url,
                             @DefaultValue("admin") String username,
                             @DefaultValue("admin123") String password) {
    }

    public record HaPolicy(@DefaultValue("ha-cola") String name,
                           @DefaultValue("^cola\\.") String pattern,
                           @DefaultValue("0") int priority,
                           @DefaultValue Map<String, Object> definition) {
    }

    public record ExchangeDef(String name, @DefaultValue("direct") String type) {
    }

    public record QueueDef(String name, @DefaultValue("false") boolean deadLetter) {
    }

    public record BindingDef(String exchange, String queue, String routingKey) {
    }
}
