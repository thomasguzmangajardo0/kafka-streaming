# Guion de presentación · 5 a 10 minutos (EP6)

La presentación dura **entre 5 y 10 minutos**: no alcanza para decir todo lo de este guion. Úselo como banco de
contenido y arme su versión con esta prioridad (los porcentajes son los de la pauta):

| Bloque | % pauta | Tiempo objetivo |
|---|---|---|
| **Parte A · Kafka** (secciones 1–8) | 100 % de la pauta | ~7 min (los tiempos de cada sección suman ~9 min: son máximos, condense) |
| **Parte B · Evidencias en la nube** (API Manager, Cognito, RabbitMQ, despliegue) | exigido por las instrucciones | ~2:30 min, recorrido rápido por la consola |

Para ganar tiempo: abra **todas las pestañas antes** (Kafka UI, consola RabbitMQ, consola AWS, frontend) y deje
los pedidos de demostración ya cargados; en cada sección muestre una cosa y explíquela, no recorra menús.
El orden sigue el hilo conductor *infraestructura → diseño → flujo → administración → operación → casos de uso → nube*.

**Antes de empezar (tenerlo todo abierto y funcionando):** los tres `docker compose` (Kafka, Kafka UI, RabbitMQ)
y los 5 microservicios corriendo (en la EC2 si presenta en la nube), Kafka UI en `:8090`, consola RabbitMQ en `:15672`,
el frontend con sesión iniciada, `export TOKEN=...` en una terminal con `./scripts/demo.sh` preparado y otra
terminal con los logs de `inventario-service`.

---

## 1. Arquitectura Kafka con Docker Compose (≈1:15) · 10 %
**Mostrar:** `docker compose -f docker/docker-compose.kafka.yml ps` y el YAML.

**Decir:**
- "Levantamos 3 nodos Zookeeper y 3 brokers con un solo compose."
- **Zookeeper** guarda los metadatos del clúster (brokers vivos, tópicos, configuración) y elige al controlador. Con **3 nodos hay quórum de 2**: si cae uno, el ensamble sigue funcionando. Con 2 nodos, caer uno dejaría sin quórum.
- Cada **broker** guarda particiones y atiende a productores y consumidores. Con 3 brokers cada partición tiene un líder y dos seguidores en brokers distintos.
- **Tolerancia a fallos:** `replication.factor=3`, `min.insync.replicas=2` y `acks=all`: toda escritura confirmada existe en al menos 2 brokers. Si cae 1 broker, otro réplica pasa a líder y el sistema sigue leyendo y escribiendo. `unclean.leader.election=false` evita promover una réplica desactualizada (se prioriza no perder datos).
- Dos listeners por broker: `INTERNAL` (entre contenedores) y `EXTERNAL` (puertos 9092/9093/9094 hacia los microservicios).

**Demo opcional de resiliencia (30 s):** `docker stop kafka2`, crear un pedido (sigue funcionando), `curl localhost:8084/api/kafka/cluster` muestra 2 brokers; `docker start kafka2`.

## 2. Dos tópicos, 3 particiones y 3 réplicas (≈1:00) · 10 %
**Mostrar:** Kafka UI → Topics → `pedidos.eventos` → Partitions (líder, réplicas, in-sync).

**Decir:**
- **Particiones = paralelismo.** Un grupo de consumidores puede tener hasta tantos consumidores activos como particiones; con 3 podemos procesar 3 pedidos en paralelo y escalar horizontalmente sin cambiar código. Si ante un gran volumen de pedidos se queda corto, se aumentan particiones desde el administrador.
- La **clave `pedidoId`** hace que todos los eventos de un pedido caigan en la misma partición → se respeta el orden por pedido.
- **Réplicas = resiliencia.** Cada partición tiene 3 copias; un broker puede caer sin perder datos ni disponibilidad.
- Tópicos: `pedidos.eventos` (hechos de pedidos) e `inventario.eventos` (resultado de stock), más sus DLT.

## 3. Kafka UI (≈0:45) · 5 %
**Mostrar:** Brokers (3 vivos), Topics, Messages de `pedidos.eventos` (JSON), Consumers.

**Decir:** "Kafka UI corre en su propio compose, en la misma red. Permite ver el estado de los brokers, crear/configurar tópicos, ver particiones y réplicas, leer mensajes con su clave/offset y monitorear consumer groups. Es la herramienta de operación del día a día."

## 4. Flujo de producción y consumo + diseño de eventos (≈1:45) · 12 % + 6 %
**Mostrar:** diagrama del README; `PedidoEvento` y `StockEvento`; logs mientras corre `demo.sh` (pasos 4–5).

**Decir (flujo):**
1. `pedido-service` recibe el POST, guarda el pedido `PENDIENTE` y **publica `PEDIDO_CREADO`**.
2. `inventario-service` (grupo `inventario-group`) lo consume, reserva stock y **publica `STOCK_RESERVADO` o `STOCK_RECHAZADO`**.
3. `pedido-service` (grupo `pedido-group`) consume ese resultado y pasa el pedido a `CONFIRMADO` o `RECHAZADO`.
4. `notificacion-service` consume **ambos** tópicos con grupos propios y genera los correos.
- "Esto resuelve el requisito de **desacoplar**: pedido no llama a inventario; si inventario está caído, los eventos esperan en Kafka y se procesan al volver."

**Decir (diseño de eventos):**
- Eventos **autocontenidos**: `PedidoEvento` lleva `eventId` (idempotencia), `tipoEvento`, `pedidoId` (clave), `clienteEmail` (para notificar sin consultar), `items` (lo que inventario necesita) y `total`, `fechaCreacion` (auditoría).
- `StockEvento` lleva `estado` + `motivo` (para explicar el rechazo), `pedidoId`, `clienteEmail` e `items`.
- "Cada campo existe porque algún caso de uso lo necesita; ningún servicio tiene que llamar a otro para completar datos."
- Serialización JSON tipada con DTO; `ErrorHandlingDeserializer` para que un JSON roto no bloquee.

## 5. Reacción en tiempo real (≈1:15) · 12 %
**Mostrar:** terminal con `./scripts/demo.sh` (o 3 `curl`): stock antes → crear pedido → a los 2–3 s el pedido está `CONFIRMADO`, el stock bajó y apareció una notificación. Luego pedido de `SKU-004` (stock 0) → `RECHAZADO` con motivo.

**Decir:** "Nadie consulta ni hace polling: cada servicio reacciona al evento cuando llega. Se actualizan estados y se generan procesos derivados (reserva de stock, correo) automáticamente."

## 6. Administración de Kafka (≈1:15) · 15 %
**Mostrar:** `kafka-admin-service/src/main/resources/application.yml`, `GET /api/kafka/topics/pedidos.eventos` (configs) y un `PATCH .../config`.

**Decir los conceptos con su aplicación:**
- **Particiones:** paralelismo (3). **Réplicas:** disponibilidad/durabilidad (3, ISR mínimo 2).
- **Retention:** `pedidos.eventos` 7 días para reproceso y auditoría; DLT 30 días para analizar fallos con calma.
- **Cleanup policy:** `delete` en pedidos (log de hechos) y `compact,delete` en `inventario.eventos`: al compactar por clave queda **el último resultado por pedido** → consistencia con menos almacenamiento.
- **Consumer groups:** `inventario-group` reparte las particiones entre instancias (balanceo y resiliencia: si una cae, las demás toman sus particiones); `notificacion-*` y `pedido-group` son grupos distintos y por eso reciben **copia** de cada evento (publish-subscribe).
- "Los tópicos se crean de forma controlada (`auto.create.topics.enable=false`): por el administrador al arrancar y por el script `crear-topicos.sh`, ambos repetibles."

## 7. Supervisión: offsets, lag y distribución (≈1:15) · 15 %
**Mostrar:** Kafka UI → Consumers → `inventario-group` (lag por partición) y `GET /api/kafka/consumer-groups/inventario-group/lag` y `.../{id}` (miembros y particiones).

**Decir:**
- **Offset:** posición de cada mensaje en la partición. **Offset confirmado:** hasta dónde llegó el grupo. **Lag = último offset − offset confirmado**.
- Lag ≈ 0: el consumidor va al día. **Lag creciente:** consumidores lentos, caídos o muy pocos → anticipa problemas *antes* de que el usuario los note; se resuelve escalando instancias (hasta 3 por las 3 particiones) o optimizando el procesamiento.
- **Distribución del consumo:** la vista del grupo muestra qué consumidor tiene qué partición; si una partición concentra todo el lag hay *skew* de claves.
- **Demo rápida (opcional):** detener `inventario-service`, crear 3 pedidos → lag = 3; levantarlo → lag vuelve a 0 y los pedidos se confirman solos.
- CLI equivalente: `docker exec kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092 --describe --group inventario-group`.

## 8. Casos de uso que requieren streaming (≈1:00) · 15 %
1. **Procesamiento de pedidos / reserva de stock** — alto volumen y picos: 3 particiones + `inventario-group` escalable, retención 7 días para reproceso.
2. **Estado actual del resultado por pedido** — `inventario.eventos` compactado: la última versión por `pedidoId` siempre disponible; un servicio nuevo puede reconstruir estado leyendo desde el inicio.
3. **Notificaciones desacopladas** — grupos propios: agregar un consumidor nuevo (analítica, facturación) no toca a pedido ni inventario.
4. **Manejo de fallos** — mensajes defectuosos a la DLT con reintentos; se investigan sin detener el flujo; retención larga.

---

# Parte B · Evidencias en la nube (≈2:30)

Detalle y capturas sugeridas en `docs/DESPLIEGUE_AWS.md` (sección 7). Recorrido rápido, en este orden:

1. **Despliegue activo (20 s):** consola EC2 con la instancia *running* y, por SSH, `docker compose ... ps` mostrando Kafka, RabbitMQ y los 5 servicios arriba; CloudFront/S3 con el frontend publicado. *"Backend y frontend desplegados, activos e integrados."*
2. **Tenant e IDaaS (25 s):** Cognito → *user pool* (el tenant) → **Users** con los usuarios registrados.
3. **Login OAuth 2.0 / OIDC (30 s):** en el frontend, *Cerrar sesión* → *Ingresar con Cognito* → Hosted UI → vuelve con sesión. Abrir *Network*: la llamada a `execute-api...` lleva `Authorization: Bearer <JWT>`. Decir: *"código de autorización con PKCE; el frontend nunca ve la clave del usuario"*.
4. **API Manager (40 s):** API Gateway → `pedidos-api` (instancia y *stage* activos) → **Routes/Integrations** (cada ruta apunta a un microservicio) → **Authorizer** JWT (issuer = Cognito, audience = app client). Mostrar en la terminal `curl` **sin token → 401**, **token falso → 401**, **token válido → 200** (`demo.sh` sección 0).
5. **RabbitMQ (50 s):** consola → *Overview*: **2 nodos** en clúster; *Queues*: **3 colas** (`cola.pedidos.confirmados`, `cola.notificaciones`, `cola.auditoria`) con sus **3 DLQ** (`*.dlq`); *Exchanges*: `pedidos.direct` (**direct**) y `eventos.topic` (**topic**) con sus *bindings*. Mostrar `demo.sh` sección 11: un mensaje defectuoso termina en `cola.notificaciones.dlq` y en el log `[DLQ]`.
6. **Kafka UI (ya mostrado en la sección 2):** los dos tópicos con 3 particiones y 3 réplicas.

**Cómo conviven Kafka y RabbitMQ (frase de cierre):** *"Kafka transporta el flujo de eventos de pedidos y stock, que
queda retenido y se puede reprocesar y leer desde varios grupos. RabbitMQ entrega tareas puntuales con ruteo
(despacho, notificación, auditoría) y manda lo fallido a una DLQ. Kafka se agregó sin modificar el uso de RabbitMQ."*

---

## Preguntas probables del docente (respuestas cortas)

- **¿Por qué Kafka además de RabbitMQ?** RabbitMQ: colas de trabajo/ruteo con mensajes que se consumen y desaparecen. Kafka: log distribuido que **retiene** eventos, permite reproceso, múltiples grupos independientes y gran volumen. Conviven y no se pisan: RabbitMQ sigue entregando las tareas puntuales (despacho, notificación, auditoría) y Kafka transporta el flujo de eventos.
- **¿Qué pasa si cae un broker?** Líderes pasan a réplicas del ISR; con `min.insync.replicas=2` y `acks=all` se sigue escribiendo con 2 brokers.
- **¿Y si caen 2?** No se pierden datos confirmados, pero se rechazan escrituras (ISR < 2) hasta recuperar uno: se prioriza consistencia.
- **¿Entrega "exactamente una vez"?** Productor idempotente evita duplicados por reintento; el consumo es "al menos una vez" y por eso inventario es idempotente por `eventId`.
- **¿Por qué no más consumidores que particiones?** Una partición la lee un solo consumidor del grupo; los extra quedan inactivos.
- **¿Por qué `acks=all`?** Esperar a todas las réplicas en sincronía; evita perder un pedido confirmado.
- **¿Qué hace la DLT?** Recibe lo que falló tras 3 reintentos (o no es reintentable, como un JSON inválido), con headers del error; se loguea `[DLQ]`.
- **¿Cómo valida el JWT el backend?** Cada microservicio es Resource Server: descarga las llaves públicas del JWKS de Cognito, verifica firma, expiración, `iss` y `client_id`; API Gateway lo valida antes (defensa en profundidad).
- **¿Qué pasa si cae un nodo de RabbitMQ?** Las colas `cola.*` están replicadas en ambos nodos (política `ha-all`) y los clientes tienen las dos direcciones, así que se reconectan al nodo vivo.
- **¿Cómo llega un mensaje a la DLQ?** El consumidor reintenta 3 veces con espera creciente; al agotarse, `DlqLoggingRecoverer` lo registra y lo rechaza sin reencolar; la cola tiene `x-dead-letter-exchange`, y RabbitMQ lo deriva a `<cola>.dlq`.
- **¿Se puede reducir particiones?** No; solo aumentarlas (puede alterar el orden por clave de ahí en adelante).
