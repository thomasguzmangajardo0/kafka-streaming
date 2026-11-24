# Despliegue en AWS (Learner Lab) · paso a paso

Equivalencias de lo que pide la evaluación con servicios de AWS:

| Requisito | Servicio AWS |
|---|---|
| IDaaS (tenant + usuarios) | **Amazon Cognito** → *user pool* |
| API Manager | **Amazon API Gateway** (HTTP API) con *JWT authorizer* |
| Base de datos cloud | **Amazon RDS** (PostgreSQL o MySQL) |
| Backend + Kafka + RabbitMQ (docker-compose) | **Amazon EC2** con Docker |
| Frontend desplegado | **S3 + CloudFront** (HTTPS) |

```
 Navegador ──► CloudFront/S3 (Angular)
     │  login OIDC (code + PKCE)
     ├────────────► Cognito Hosted UI ──► JWT
     │
     └─ Bearer JWT ► API Gateway (valida JWT) ──► EC2 :8081-8085 (valida JWT otra vez)
                                                   ├─ pedido / inventario / notificacion / admins
                                                   ├─ Kafka  (3 ZK + 3 brokers) + Kafka UI :8090
                                                   ├─ RabbitMQ (2 nodos)        + consola :15672
                                                   └──► RDS (tabla pedidos)
```

> **Learner Lab:** la disponibilidad de algunos servicios (Cognito, CloudFront, API Gateway) y los permisos
> varían según el laboratorio. Verifique en su consola cuáles aparecen antes de empezar; si CloudFront no está
> disponible vea la alternativa del paso 6. Las sesiones del laboratorio expiran: al volver, revise que la IP
> pública de la EC2 y las instancias sigan activas.

Región usada en los ejemplos: `us-east-1`.

---

## 1. Amazon Cognito (IDaaS)

1. **Cognito → Create user pool**. Sign-in: *email*. Políticas por defecto. MFA: no.
   - Este *user pool* es el **tenant** que debe mostrar en la presentación.
2. Dentro del pool: **App integration → Domain** → cree un dominio de Cognito (p. ej. `pedidos-duoc-123`).
3. **App clients → Create app client**
   - Tipo: *Single-page application (public client)*, **sin client secret**.
   - Flujos: *Authorization code grant* (PKCE). Scopes: `openid`, `email`, `profile`.
   - **Allowed callback URLs**: `http://localhost:4200` y `https://<distribución>.cloudfront.net`
   - **Allowed sign-out URLs**: las mismas.
4. **Users → Create user**: cree 2 o 3 usuarios de prueba con correo y clave (evidencia: *"existen usuarios registrados"*).
5. Anote:
   - `userPoolId` → p. ej. `us-east-1_AbCdEfGhI`
   - `clientId` del app client
   - **Issuer** = `https://cognito-idp.us-east-1.amazonaws.com/<userPoolId>`
   - Dominio Hosted UI = `https://<dominio>.auth.us-east-1.amazoncognito.com`

Comprobación: abra `<issuer>/.well-known/openid-configuration` en el navegador; debe devolver un JSON.

## 2. Amazon RDS (base de datos)

1. **RDS → Create database** → *Standard create* → PostgreSQL (o MySQL), plantilla *Free tier / Dev*.
2. Nombre de BD inicial: `pedidosdb`. Usuario/clave maestros (anótelos).
3. Conectividad: misma VPC que la EC2; *Public access*: **No**.
4. Security group de RDS: regla de entrada `5432` (o `3306`) con **origen = el security group de la EC2**.
5. Cadena JDBC: `jdbc:postgresql://<endpoint>:5432/pedidosdb`. Las tablas (`pedidos`, `pedido_items`) las crea
   Hibernate al iniciar `pedido-service` (`DB_DDL_AUTO=update`).

## 3. Amazon EC2 (Kafka + RabbitMQ + microservicios)

1. **Lanzar instancia**: Amazon Linux 2023, **`t3.large` (8 GB) como mínimo** (`t3.xlarge` si el laboratorio lo permite),
   disco 30 GB. Sin memoria suficiente, Kafka y los servicios se reinician: es el problema más común.
2. **Security group** (entrada):

   | Puerto | Origen | Para qué |
   |---|---|---|
   | 22 | su IP | SSH |
   | 8081–8085 | 0.0.0.0/0 (API Gateway necesita alcanzarlos; el JWT protege igualmente) | microservicios |
   | 8090 | su IP | Kafka UI |
   | 15672 | su IP | consola RabbitMQ |
   | 80 | 0.0.0.0/0 | solo si usa el frontend en nginx |

3. Asigne una **Elastic IP** (la IP pública cambia al detener la instancia y rompería las rutas de API Gateway).
4. Instale Docker:

   ```bash
   sudo dnf install -y docker git
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user && newgrp docker
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   docker compose version
   ```
5. Clone el repositorio y configure:

   ```bash
   git clone <url-del-repo> kafka-streaming && cd kafka-streaming
   cp .env.example .env && nano .env      # IDAAS_*, DB_*, RABBIT_*, CORS_ALLOWED_ORIGINS
   ```
6. Levante en este orden (esperar a que cada clúster quede *healthy*):

   ```bash
   docker compose -f docker/docker-compose.kafka.yml up -d        # 3 ZK + 3 brokers + creación de tópicos
   docker compose -f docker/docker-compose.kafka-ui.yml up -d     # Kafka UI  :8090
   docker compose -f docker/docker-compose.rabbitmq.yml up -d     # 2 nodos RabbitMQ :15672 / :15673
   docker compose --env-file .env -f docker/docker-compose.apps.yml up -d --build
   docker compose -f docker/docker-compose.kafka.yml ps           # 6 contenedores healthy
   ```
   La primera construcción de las imágenes Maven tarda varios minutos. Si la EC2 se queda sin memoria al
   compilar, construya antes con `--build` de a un servicio (`... up -d --build pedido-service`).
7. Prueba rápida (desde la EC2): `curl -i localhost:8081/api/pedidos` → **401** (correcto: exige JWT).
   `curl localhost:8081/actuator/health` → `{"status":"UP"}`.

## 4. API Gateway (API Manager)

1. **API Gateway → Create API → HTTP API**. Nombre: `pedidos-api`.
2. **Integrations → Create** (tipo *HTTP*, método ANY). Una por servicio (use la Elastic IP):

   | Integración | URL |
   |---|---|
   | `pedidos` | `http://<EIP>:8081/api/pedidos` |
   | `inventario` | `http://<EIP>:8082/api/inventario` |
   | `notificaciones` | `http://<EIP>:8083/api/notificaciones` |
   | `kafka-admin` | `http://<EIP>:8084/api/kafka` |
   | `rabbit-admin` | `http://<EIP>:8085/api/rabbit` |

3. **Routes** (todas con método `ANY`; para cada recurso dos rutas, una exacta y otra con `{proxy+}`):

   | Ruta | Integración | URL de la integración |
   |---|---|---|
   | `ANY /api/pedidos` | pedidos | `http://<EIP>:8081/api/pedidos` |
   | `ANY /api/pedidos/{proxy+}` | pedidos | `http://<EIP>:8081/api/pedidos/{proxy}` |
   | `ANY /api/inventario`, `ANY /api/inventario/{proxy+}` | inventario | `http://<EIP>:8082/api/inventario[/{proxy}]` |
   | `ANY /api/notificaciones`, `ANY /api/notificaciones/{proxy+}` | notificaciones | `http://<EIP>:8083/api/notificaciones[/{proxy}]` |
   | `ANY /api/kafka/{proxy+}` | kafka-admin | `http://<EIP>:8084/api/kafka/{proxy}` |
   | `ANY /api/rabbit/{proxy+}` | rabbit-admin | `http://<EIP>:8085/api/rabbit/{proxy}` |

4. **Authorization → Create JWT authorizer**
   - Identity source: `$request.header.Authorization`
   - Issuer URL: `https://cognito-idp.us-east-1.amazonaws.com/<userPoolId>`
   - Audience: el **clientId** del app client
   - Asócielo a **todas** las rutas. Resultado: API Gateway rechaza con **401** antes de llegar al backend.
5. **CORS** (en la API): *Allow origin* = URL de CloudFront y `http://localhost:4200`; *Allow headers* =
   `authorization,content-type`; *Allow methods* = `GET,POST,PUT,PATCH,DELETE,OPTIONS`.
6. **Stages**: deje `$default` con *auto-deploy*. La URL de invocación (`https://<id>.execute-api.us-east-1.amazonaws.com`)
   es la que va en `apiBaseUrl` del frontend.

Prueba:

```bash
TOKEN=$(COGNITO_CLIENT_ID=<clientId> ./scripts/obtener-token.sh usuario@correo.cl 'Clave123!')
curl -i  https://<id>.execute-api.us-east-1.amazonaws.com/api/pedidos                         # 401
curl -i -H "Authorization: Bearer $TOKEN" https://<id>.execute-api.us-east-1.amazonaws.com/api/pedidos  # 200
```
(`obtener-token.sh` requiere habilitar `ALLOW_USER_PASSWORD_AUTH` en un app client de pruebas.)

## 5. Cómo se valida el JWT en el backend (defensa en profundidad)

Aunque API Gateway ya lo valida, cada microservicio vuelve a validarlo (`SecurityConfig`, Spring Security Resource Server):
firma con las llaves públicas de `<issuer>/.well-known/jwks.json`, expiración, `iss` y que `client_id`/`aud` sea
el app client (`IDAAS_AUDIENCE`). Si alguien llama directo a `:8081` sin token válido, recibe **401**.

## 6. Frontend (S3 + CloudFront)

1. Edite `frontend/src/environments/environment.prod.ts` con: URL de API Gateway, `authority` (issuer),
   `clientId`, dominio de Cognito y la URL de CloudFront (`redirectUrl`, `postLogoutRedirectUri`).
2. Compile: `cd frontend && npm ci && npm run build` → `dist/frontend/browser`.
3. **S3**: bucket privado → `aws s3 sync dist/frontend/browser s3://<bucket> --delete`.
4. **CloudFront**: origen = bucket S3 con *Origin Access Control*, *Default root object* = `index.html`,
   *Viewer protocol* = Redirect HTTP→HTTPS. En **Error pages** agregue 403 y 404 → `/index.html` con código 200
   (rutas del SPA). Copie la URL `https://dxxxx.cloudfront.net` y regístrela en el app client de Cognito.
5. Si **CloudFront no está disponible** en su laboratorio:
   - *Opción A:* `docker compose --env-file .env -f docker/docker-compose.apps.yml --profile frontend up -d --build`
     sirve el frontend en la EC2 (puerto 80). Cognito exige HTTPS en las *callback URL* (salvo `localhost`), así
     que esta opción solo sirve si dispone de HTTPS (dominio + certificado).
   - *Opción B (demo):* ejecutar el frontend en `http://localhost:4200` apuntando al API Gateway en la nube.

## 7. Qué mostrar en la presentación (consola AWS) → ver `GUION_PRESENTACION.md`

| Evidencia que pide la EP6 | Dónde mostrarla |
|---|---|
| Instancia de API Manager en funcionamiento | API Gateway → `pedidos-api` → Stages |
| Configuración que permite llamar a los endpoints | API Gateway → Routes + Integrations |
| Frontend consume vía API Manager | Navegador → pestaña *Network*: las llamadas van a `execute-api...` |
| API Manager valida JWT | Request con y sin token (401 / 200) |
| Tenant en IDaaS y usuarios | Cognito → User pool → Users |
| OAuth2/OIDC desde el frontend | Pantalla de login de Cognito → vuelve con sesión; en *Network* el header `Authorization: Bearer` |
| Backend y frontend desplegados | EC2 *running* + `docker compose ps`; CloudFront/S3 |
| 2 nodos RabbitMQ en clúster | Consola `:15672` → Overview → Nodes (2) |
| 3 colas con DLQ | Consola → Queues (3 `cola.*` + 3 `*.dlq`) |
| Exchanges direct y topic | Consola → Exchanges (`pedidos.direct`, `eventos.topic`) |
| docker-compose ejecutándose en la nube | SSH a la EC2: `docker compose ... ps` |
| Kafka UI con 2 tópicos (3 part./3 rép.) | `http://<EIP>:8090` → Topics |

## 8. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| 401 con token recién emitido | `IDAAS_ISSUER_URI` distinto al `iss` del token (revise región/poolId) o `IDAAS_AUDIENCE` ≠ clientId. Pruebe con el *access token*, no con el de refresco. |
| 401 en API Gateway pero no en el backend | El *Audience* del authorizer no es el clientId; o falta `Bearer ` en el header. |
| `502/503` desde API Gateway | Security group de la EC2 no deja entrar a 8081–8085, o el contenedor no está arriba (`docker ps`). |
| Error CORS en el navegador | Falta el origen en la configuración CORS de API Gateway (y en `CORS_ALLOWED_ORIGINS`). |
| Cognito: `redirect_mismatch` | La URL de retorno no coincide exactamente con *Allowed callback URLs* (incluya http/https y sin `/` final). |
| Servicios reiniciándose | Falta de RAM: use `t3.large`/`xlarge`; revise `docker stats` y `free -m`. |
| `pedido-service` no arranca | `DB_URL`/credenciales mal, o el security group de RDS no admite a la EC2. |
| Colas no existen | `rabbit-admin-service` crea la topología al iniciar; reaplique con `POST /api/rabbit/topologia/aplicar`. |

## 9. Costos y limpieza

Detenga la EC2 y la BD RDS al terminar (*Stop*), libere la Elastic IP si no la usa y elimine el bucket/CloudFront
cuando ya no los necesite.
