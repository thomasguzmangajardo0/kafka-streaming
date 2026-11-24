# Sistema de pedidos orientado a eventos · Kafka + RabbitMQ + Cognito + API Gateway (DSY1107 · EP5 y EP6)

Backend con microservicios **Java 17 / Spring Boot 3.4**, frontend **Angular 22**, mensajería con
**Kafka** (clúster 3 ZK + 3 brokers) y **RabbitMQ** (clúster de 2 nodos), autenticación con **Amazon Cognito**
(OAuth 2.0 / OIDC, JWT), **API Gateway** como API Manager y **Amazon RDS** como base de datos cloud.

| Tecnología | Para qué se usa aquí |
|---|---|
| **Kafka** | Flujo de eventos de pedidos y stock (alto volumen, reproceso, varios consumidores independientes) |
| **RabbitMQ** | Tareas puntuales entre servicios: despacho, notificaciones y auditoría con ruteo (direct/topic) y DLQ |
| **Cognito** | Login del usuario (tenant = *user pool*) y emisión del JWT |
| **API Gateway** | Único punto de entrada; valida el JWT antes de llegar al backend |
| **RDS** | Persistencia de los pedidos (JPA) |

## Arquitectura

```
 Angular ──login OIDC──► Cognito ──JWT──┐
    │                                    ▼
    └──── Bearer JWT ─────────► API Gateway (valida JWT) ──► microservicios (validan JWT otra vez)

                  pedidos.eventos (3 part., 3 réplicas, delete 7d)             Kafka
 ┌───────────────┐   key = pedidoId   ┌────────────────────┐
 │ pedido-service│ ─────────────────▶ │ inventario-service │  grupo: inventario-group
 │     :8081     │                    │       :8082        │
 └──▲─────┬──────┘ ◀───────────────── └──────────┬─────────┘
    │     │      inventario.eventos (3 part., 3 réplicas, compact,delete)
    │     │ Mensaje "pedido finalizado"                                       RabbitMQ
    │     ├──► pedidos.direct (direct) ─ pedido.confirmado ─► cola.pedidos.confirmados ─► inventario (despacho)
    │     └──► eventos.topic  (topic)  ─ pedido.*  ─► cola.notificaciones ─┐
    │                                  ─ pedido.#  ─► cola.auditoria      ─┴► notificacion-service
    │                          (cada cola ──fallo──► dlx.direct ──► <cola>.dlq)
    ▼ JPA
   RDS      notificacion-service :8083 (lee ambos tópicos Kafka + 2 colas RabbitMQ)
            kafka-admin-service  :8084   rabbit-admin-service :8085
```

**Flujo:** `POST /api/pedidos` (202) → evento Kafka `PEDIDO_CREADO` → inventario reserva o rechaza →
evento `STOCK_RESERVADO/RECHAZADO` → el pedido pasa a `CONFIRMADO/RECHAZADO` (se guarda en RDS) y
`pedido-service` publica en RabbitMQ el resultado final → despacho, notificación y auditoría. Todo asíncrono.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `docker/` | Compose de **Kafka** (3 ZK + 3 brokers + `kafka-init`), **Kafka UI**, **RabbitMQ** (2 nodos) y de las **apps** |
| `pedido-service/` | API de pedidos + JPA/RDS; produce `pedidos.eventos`, consume `inventario.eventos`; publica en RabbitMQ |
| `inventario-service/` | Consume `pedidos.eventos`, produce `inventario.eventos`; consume `cola.pedidos.confirmados` |
| `notificacion-service/` | Consume ambos tópicos (grupos propios) y las colas `cola.notificaciones` y `cola.auditoria` |
| `kafka-admin-service/` | API REST de administración de Kafka (tópicos, configs, consumer groups, lag) |
| `rabbit-admin-service/` | API REST de administración de RabbitMQ (colas, exchanges, bindings, políticas, clúster) |
| `frontend/` | Angular: login OIDC con Cognito, JWT en las llamadas, vistas de pedidos/inventario/notificaciones |
| `scripts/` | `crear-topicos.sh`, `demo.sh`, `obtener-token.sh` |
| `docs/` | `DESPLIEGUE_AWS.md`, `GUION_PRESENTACION.md`, `MATRIZ_RUBRICA.md` |

Cada microservicio es un proyecto Maven independiente (Java 17, Spring Boot 3.4.5) con su `Dockerfile`.
Si el docente exige **repositorios separados** (backend / frontend), `frontend/` se sube como su propio
repositorio (trae su `.gitignore` de Angular) y el resto como repositorio de backend. El `.gitignore` raíz
excluye `target/`, `node_modules/`, `.env`, etc.

## Ejecución local

Requisitos: Docker, JDK 17, Maven, Node 22.

```bash
# 1) Mensajería
docker compose -f docker/docker-compose.kafka.yml up -d       # 3 ZK + 3 brokers; kafka-init crea los tópicos
docker compose -f docker/docker-compose.kafka-ui.yml up -d    # Kafka UI -> http://localhost:8090
docker compose -f docker/docker-compose.rabbitmq.yml up -d    # RabbitMQ -> http://localhost:15672 (admin/admin123)

# 2) Seguridad: sin IDaaS configurado todas las rutas responden 401. Defina el issuer de su user pool:
export IDAAS_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/<userPoolId>
export IDAAS_AUDIENCE=<appClientId>

# 3) Microservicios (una terminal cada uno). pedido-service usa H2 en memoria si no define DB_URL.
cd rabbit-admin-service && mvn spring-boot:run    # :8085  crea exchanges, 3 colas + 3 DLQ y la política HA
cd kafka-admin-service  && mvn spring-boot:run    # :8084  crea/corrige tópicos al iniciar
cd inventario-service   && mvn spring-boot:run    # :8082
cd pedido-service       && mvn spring-boot:run    # :8081
cd notificacion-service && mvn spring-boot:run    # :8083

# 4) Frontend (configure src/environments/environment.ts antes)
cd frontend && npm install && npm start           # http://localhost:4200

# 5) Demo por terminal
export TOKEN=$(COGNITO_CLIENT_ID=<id> ./scripts/obtener-token.sh usuario@correo.cl 'Clave123!')
./scripts/demo.sh
```

Variables útiles: `KAFKA_BOOTSTRAP_SERVERS`, `RABBIT_ADDRESSES`, `RABBIT_USER/PASSWORD`, `DB_URL/DB_USERNAME/DB_PASSWORD`,
`IDAAS_ISSUER_URI`, `IDAAS_AUDIENCE`, `CORS_ALLOWED_ORIGINS`. Ver `.env.example`.

Pruebas: `mvn test` en cada servicio y `npm run build` en el frontend. `inventario-service` incluye una prueba
de integración con Kafka embebido (`InventarioKafkaFlowTest`); para omitirla: `mvn test -Dtest='!InventarioKafkaFlowTest'`.

**Despliegue en AWS (Cognito, RDS, EC2, API Gateway, S3/CloudFront): ver [`docs/DESPLIEGUE_AWS.md`](docs/DESPLIEGUE_AWS.md).**

## Seguridad (JWT)

Cada microservicio es *Resource Server* OAuth2: el filtro `BearerTokenAuthenticationFilter` valida firma
(JWKS de Cognito), expiración, `iss` y `client_id`/`aud` en cada petición; sin token válido → **401**.
`/actuator/health` es público (health checks). El frontend obtiene el token con *authorization code + PKCE*
y lo envía solo a la URL del API Manager (`secureRoutes`). CORS configurable por `CORS_ALLOWED_ORIGINS`.

## Kafka · decisiones de diseño

| Decisión | Justificación |
|---|---|
| 3 Zookeeper | Quórum 2/3: el ensamble sigue operando si cae un nodo |
| 3 brokers, réplicas=3, `min.insync.replicas=2`, productor `acks=all` | Sin pérdida de datos ante la caída de 1 broker y el sistema sigue aceptando escrituras |
| 3 particiones | Paralelismo: hasta 3 consumidores activos por grupo; mismo `pedidoId` → misma partición (orden por pedido) |
| `pedidos.eventos` con `cleanup.policy=delete`, 7 días | Es un log de hechos: sirve para reproceso y auditoría |
| `inventario.eventos` con `compact,delete`, 30 días | Solo importa el último resultado por pedido (clave = `pedidoId`) |
| Productor idempotente + `acks=all` | Sin duplicados por reintentos del productor |
| Consumidor con commit por registro, `CooperativeStickyAssignor` | No se pierde ni se salta un mensaje; rebalanceos sin detener todo el grupo |
| `ErrorHandlingDeserializer` + backoff exponencial + DLT | Un mensaje malformado o que falla nunca bloquea la partición |
| Idempotencia por `eventId` en inventario | Kafka garantiza "al menos una vez": una entrega duplicada no descuenta stock dos veces |
| Puertos (`PedidoEventPublisher`, `StockEventPublisher`, `PedidoFinalizadoPublisher`) | La lógica de negocio no conoce Kafka ni RabbitMQ; los listeners solo reciben y delegan |
| Grupos distintos por servicio | `inventario-group` reparte carga; `notificacion-*` y `pedido-group` reciben copia de cada evento |

## RabbitMQ · decisiones de diseño

| Decisión | Justificación |
|---|---|
| Clúster de 2 nodos (`classic_config`) + política `ha-all` sobre `cola.*` | Cada cola tiene réplica en ambos nodos; si cae uno, el otro sigue sirviendo |
| Exchange **direct** `pedidos.direct` | Ruteo exacto: solo los pedidos *confirmados* van a despacho (`pedido.confirmado`) |
| Exchange **topic** `eventos.topic` | `pedido.*` → notificaciones; `pedido.#` → auditoría: un mismo mensaje llega a varias colas |
| 3 colas, cada una con `x-dead-letter-exchange` → `dlx.direct` → `<cola>.dlq` | Lo que falla no se pierde ni se reencola en bucle |
| Consumidores: 3 reintentos con espera creciente + `DlqLoggingRecoverer` | Se registra en log `[DLQ]` (cola, clave, causa, cuerpo) y se rechaza sin reencolar |
| Productor con *publisher confirms* y `mandatory` | Un mensaje no enrutable queda en el log en vez de perderse en silencio |
| Mensajes idempotentes por `mensajeId` | RabbitMQ entrega "al menos una vez" |

## API de los administradores

**`kafka-admin-service` (:8084)**

| Método y ruta | Acción |
|---|---|
| `GET /api/kafka/cluster` | Brokers vivos y controlador |
| `GET/POST /api/kafka/topics` | Listar / crear tópicos |
| `GET /api/kafka/topics/{name}` | Particiones (líder, réplicas, ISR) y configuraciones |
| `PATCH /api/kafka/topics/{name}/config` | Cambiar `retention.ms`, `cleanup.policy`, etc. |
| `PUT /api/kafka/topics/{name}/partitions` | Aumentar particiones |
| `DELETE /api/kafka/topics/{name}` | Eliminar tópico |
| `GET /api/kafka/consumer-groups[/{id}[/lag]]` | Grupos, miembros y particiones asignadas, offsets y lag |
| `POST /api/kafka/consumer-groups/{id}/reset-offsets` | Reubicar offsets (grupo inactivo) |
| `DELETE /api/kafka/consumer-groups/{id}` | Eliminar grupo inactivo |

**`rabbit-admin-service` (:8085)**

| Método y ruta | Acción |
|---|---|
| `GET /api/rabbit/cluster` | Nodos del clúster y su estado |
| `GET /api/rabbit/queues` · `/exchanges` · `/bindings` · `/policies` | Inspección |
| `POST /api/rabbit/queues` (`{"name":"cola.x","conDlq":true}`) / `DELETE /api/rabbit/queues/{name}` | Crear (con su DLQ) / eliminar cola |
| `POST /api/rabbit/exchanges` / `DELETE /api/rabbit/exchanges/{name}` | Crear / eliminar exchange |
| `POST` / `DELETE /api/rabbit/bindings` | Crear / eliminar binding |
| `PUT /api/rabbit/policies/{name}` | Aplicar política (p. ej. HA) |
| `POST /api/rabbit/topologia/aplicar` | Reaplicar la topología de `application.yml` (idempotente) |

## Alcance y límites

- El frontend se compiló con `ng build` y se revisó visualmente. Los microservicios Java se revisaron
  sintácticamente, pero **no se compilaron ni se ejecutaron sus pruebas** (el entorno de desarrollo no tenía acceso a
  Maven Central): ejecute `mvn test` en cada servicio antes de entregar.
- `inventario-service` y `notificacion-service` guardan sus datos en memoria (stock, historiales); solo
  `pedido-service` persiste en la base de datos cloud.
- La configuración de AWS (Cognito, API Gateway, RDS, EC2, CloudFront) se hace en su cuenta siguiendo
  `docs/DESPLIEGUE_AWS.md`; el repositorio no contiene credenciales.
