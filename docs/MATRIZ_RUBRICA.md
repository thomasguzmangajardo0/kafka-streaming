# Matriz rúbrica → evidencia en el código

## EP5 · Encargo (código)

| % | Indicador | Dónde está |
|---|---|---|
| 12 | Configuración completa del cliente Kafka (brokers, serializers, grupos, props avanzadas) | `*/config/KafkaProducerConfig`, `KafkaConsumerConfig`, `KafkaClientProperties` + `application.yml` (`app.kafka.*`). Props avanzadas: `acks=all`, idempotencia, `linger.ms`, `batch.size`, compresión `lz4`, `CooperativeStickyAssignor`, `max.poll.*`, `session.timeout`, `read_committed`. Conecta a los 3 brokers del compose (`localhost:9092,9093,9094`). |
| 12 | Productores y consumidores sobre los **dos** tópicos, con DTO tipados | `pedido-service` (produce `pedidos.eventos`, consume `inventario.eventos`), `inventario-service` (consume `pedidos.eventos`, produce `inventario.eventos`), `notificacion-service` (consume ambos). DTO: `PedidoEvento`, `StockEvento`, `ItemPedido`, `EstadoStock`. |
| 6 | Microservicio administrador: crear, listar, mantener tópicos y configuraciones | `kafka-admin-service`: `TopicController` + `TopicAdminService` (crear, listar, describir, `PATCH config`, aumentar particiones, eliminar) |
| 14 | Productores Kafka correctos con DTO y serializers adecuados | `KafkaPedidoEventPublisher`, `KafkaStockEventPublisher` (clave = `pedidoId`, espera confirmación, errores como `EventoNoPublicadoException`); `JsonSerializer` con `ObjectMapper` de Boot, `StringSerializer` para la clave |
| 14 | Consumidores con `@KafkaListener`, lógica separada de la mensajería | `StockEventListener`, `PedidoEventListener`, `NotificacionListener` solo reciben y delegan en `PedidoService` / `InventarioService` / `NotificacionService`. Los servicios dependen de interfaces (`PedidoEventPublisher`, `StockEventPublisher`), no de Kafka |
| 12 | Manejo de errores: logging, validación de payload, reintentos | `KafkaErrorHandlingConfig`: `DefaultErrorHandler` + backoff exponencial (3 reintentos) + `DeadLetterPublishingRecoverer` → `<topico>.DLT`; `DeadLetterLogger` (log `[DLQ]` con topic/partición/offset/causa); `ErrorHandlingDeserializer`; `PedidoEventoValidator`; excepciones no reintentables; idempotencia por `eventId` |
| 12 | Parámetros de administración de tópicos (retención, cleanup, particiones, réplicas) | `kafka-admin-service/src/main/resources/application.yml` (`app.kafka.topics`) + `TopicDefinition`; también `scripts/crear-topicos.sh` y `kafka-init` del compose |
| 12 | Consumer groups para balanceo / resiliencia | `inventario-group` (concurrency 3 = 3 particiones, reparte carga entre instancias), `pedido-group`, `notificacion-pedidos-group`, `notificacion-stock-group` (fan-out). Configurados en `application.yml` y `@KafkaListener(groupId=...)` |
| 6 | Administrador o scripts para crear, actualizar, inspeccionar tópicos y grupos | `ConsumerGroupAdminService` (listar, describir, **lag**, reset de offsets, eliminar), `ClusterAdminService`, `scripts/crear-topicos.sh` (replicable) |

## EP5 · Requisitos de las instrucciones específicas (no puntúan en la rúbrica, pero se piden)

| Requisito | Dónde está |
|---|---|
| Código backend compila, buenas prácticas, pruebas básicas | 5 microservicios Maven con pruebas JUnit/Mockito/MockMvc/DataJpaTest |
| Frontend completo, modular, vistas funcionales | `frontend/` (Angular 22, componentes standalone, rutas con *lazy loading*) |
| BD cloud: entidades, repositorios, propiedades de conexión | `pedido-service/.../repository/jpa/` (`PedidoEntity`, `PedidoJpaRepository`) + `spring.datasource` en `application.yml` (variables `DB_*` → Amazon RDS) |
| Filtros que validan el JWT del IDaaS | `*/config/SecurityConfig.java` (Resource Server OAuth2) en los 5 servicios; pruebas `SeguridadJwtTest` / `SecurityConfigTest` |
| Frontend: login con IDaaS y JWT en las llamadas | `frontend/src/app/app.config.ts` (`provideAuth`, `authInterceptor`), `core/auth.service.ts`, `core/auth.guard.ts` |
| `.gitignore` por tecnología | `.gitignore` raíz (Java/Maven) y `frontend/.gitignore` (Angular) |
| Productores y consumidores RabbitMQ | `pedido-service` (`RabbitPedidoFinalizadoPublisher`), `inventario-service` (`PedidoConfirmadoRabbitListener`), `notificacion-service` (`PedidoFinalizadoRabbitListener`) |
| Mensajes no entregados a DLQ + log | colas con `x-dead-letter-exchange`; `DlqLoggingRecoverer` (log `[DLQ]`) + reintentos en `application.yml` |
| Admin RabbitMQ: crear/eliminar colas, exchanges, bindings | `rabbit-admin-service` (`RabbitAdminController`, `TopologiaService`) |
| Productores y consumidores Kafka | ver tabla de arriba |
| Admin Kafka: crear, modificar, eliminar tópicos y configs | `kafka-admin-service` (`TopicController`) |
| docker-compose RabbitMQ (clúster) | `docker/docker-compose.rabbitmq.yml` |
| docker-compose Kafka (ZK + brokers) | `docker/docker-compose.kafka.yml` |
| docker-compose Kafka UI | `docker/docker-compose.kafka-ui.yml` |
| Backend: varios microservicios Java + Spring Boot; frontend Angular | ✔ |

## EP6 · Presentación (qué mostrar)

| % | Indicador | Qué mostrar / decir (detalle en `GUION_PRESENTACION.md`) |
|---|---|---|
| 10 | Arquitectura Kafka con Docker Compose | `docker compose ps`: 3 ZK + 3 brokers; rol de cada uno; tolerancia a fallos |
| 10 | Dos tópicos, 3 particiones, 3 réplicas | Kafka UI → Topics; justificar paralelismo y resiliencia |
| 5 | Kafka UI | Brokers, tópicos, particiones, mensajes, consumers |
| 12 | Flujo producción/consumo | `demo.sh` pasos 4–6 + logs de los servicios |
| 6 | Diseño de eventos | `PedidoEvento` / `StockEvento`: por qué cada campo |
| 12 | Reacción en tiempo real | Crear pedido y ver estado, stock y notificaciones cambiar solos |
| 15 | Administración de Kafka | Particiones, réplicas, retention, cleanup, consumer groups |
| 15 | Supervisión: offsets, lag, distribución | Kafka UI → Consumers y `GET .../lag` |
| 15 | Casos de uso que requieren streaming | Tres casos y su configuración (ver guion) |

### EP6 · Evidencias que exigen las instrucciones (en la nube)

Las pide el enunciado aunque la pauta de puntaje esté centrada en Kafka. Cómo y dónde mostrarlas:
`docs/DESPLIEGUE_AWS.md` (sección 7) y la **parte B** de `docs/GUION_PRESENTACION.md`.
