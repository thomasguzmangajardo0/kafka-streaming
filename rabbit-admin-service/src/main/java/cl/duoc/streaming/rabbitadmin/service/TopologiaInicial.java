package cl.duoc.streaming.rabbitadmin.service;

import cl.duoc.streaming.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.streaming.rabbitadmin.dto.PolicyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al iniciar deja el broker como indica application.yml: exchanges, colas con DLQ, bindings y política HA. */
@Component
public class TopologiaInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopologiaInicial.class);

    private final TopologiaService topologia;
    private final RabbitManagementClient management;
    private final RabbitAdminProperties props;

    public TopologiaInicial(TopologiaService topologia, RabbitManagementClient management, RabbitAdminProperties props) {
        this.topologia = topologia;
        this.management = management;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.applyOnStartup()) {
            log.info("apply-on-startup=false: no se aplica la topología inicial");
            return;
        }
        try {
            aplicar();
        } catch (RuntimeException e) {
            // El servicio debe poder iniciar aunque el broker aún no esté listo: se puede reintentar por la API
            log.warn("No se pudo aplicar la topología al iniciar ({}). Use POST /api/rabbit/topologia/aplicar", e.getMessage());
        }
    }

    /** Reutilizable desde el endpoint POST /api/rabbit/topologia/aplicar. */
    public int aplicar() {
        int operaciones = topologia.aplicarTopologiaConfigurada();
        log.info("Topología aplicada ({} operaciones)", operaciones);
        RabbitAdminProperties.HaPolicy ha = props.haPolicy();
        if (ha != null && !ha.definition().isEmpty()) {
            try {
                management.aplicarPolitica(ha.name(), new PolicyRequest(ha.pattern(), ha.definition(), ha.priority()));
                log.info("Política HA '{}' aplicada sobre {}", ha.name(), ha.pattern());
            } catch (RuntimeException e) {
                log.warn("No se pudo aplicar la política HA (¿API de administración disponible?): {}", e.getMessage());
            }
        }
        return operaciones;
    }
}
