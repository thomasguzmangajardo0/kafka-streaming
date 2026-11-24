#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Demo de punta a punta (clúster Kafka + clúster RabbitMQ + 5 microservicios levantados).
# Cada bloque corresponde a algo que se muestra en la presentación.
#
#   export TOKEN=$(./scripts/obtener-token.sh usuario@correo.cl 'Clave123!')
#   ./scripts/demo.sh                                   # directo a los microservicios (localhost)
#   API_URL=https://<id>.execute-api.<región>.amazonaws.com ./scripts/demo.sh   # a través del API Manager
# ---------------------------------------------------------------------------
set -euo pipefail

: "${TOKEN:?Defina TOKEN con un JWT válido (ver scripts/obtener-token.sh)}"

if [[ -n "${API_URL:-}" ]]; then
  PEDIDOS="$API_URL"; INVENTARIO="$API_URL"; NOTIF="$API_URL"; KADMIN="$API_URL"; RADMIN="$API_URL"
else
  PEDIDOS=http://localhost:8081; INVENTARIO=http://localhost:8082; NOTIF=http://localhost:8083
  KADMIN=http://localhost:8084; RADMIN=http://localhost:8085
fi
RABBIT_MGMT="${RABBIT_MGMT:-http://localhost:15672}"
RABBIT_USER="${RABBIT_USER:-admin}"; RABBIT_PASSWORD="${RABBIT_PASSWORD:-admin123}"

json() { python3 -m json.tool 2>/dev/null || cat; }
api() { curl -s -H "Authorization: Bearer $TOKEN" "$@"; }
codigo() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo "== 0. Seguridad: el JWT es obligatorio =="
echo -n "sin token            -> HTTP "; codigo "$PEDIDOS/api/pedidos"; echo
echo -n "token inválido       -> HTTP "; codigo -H 'Authorization: Bearer token.falso.jwt' "$PEDIDOS/api/pedidos"; echo
echo -n "token válido         -> HTTP "; codigo -H "Authorization: Bearer $TOKEN" "$PEDIDOS/api/pedidos"; echo

echo; echo "== 1. Estado del clúster Kafka (3 brokers) =="
api "$KADMIN/api/kafka/cluster" | json

echo; echo "== 2. Tópicos del sistema (3 particiones / 3 réplicas) =="
api "$KADMIN/api/kafka/topics" | json
api "$KADMIN/api/kafka/topics/pedidos.eventos" | json

echo; echo "== 3. Stock antes =="
api "$INVENTARIO/api/inventario" | json

echo; echo "== 4. Crear pedido válido (SKU-001 x2) -> flujo por eventos =="
RESP=$(api -X POST "$PEDIDOS/api/pedidos" -H 'Content-Type: application/json' -d '{
  "clienteId":"cli-1","clienteEmail":"alumno@duoc.cl",
  "items":[{"sku":"SKU-001","cantidad":2,"precioUnitario":1500}]}')
echo "$RESP" | json
ID=$(echo "$RESP" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
sleep 4
echo "-- estado del pedido (debería ser CONFIRMADO, actualizado por evento) --"
api "$PEDIDOS/api/pedidos/$ID" | json

echo; echo "== 5. Pedido sin stock (SKU-004 = 0) -> RECHAZADO =="
RESP2=$(api -X POST "$PEDIDOS/api/pedidos" -H 'Content-Type: application/json' -d '{
  "clienteId":"cli-2","clienteEmail":"otro@duoc.cl",
  "items":[{"sku":"SKU-004","cantidad":1,"precioUnitario":990}]}')
ID2=$(echo "$RESP2" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
sleep 4
api "$PEDIDOS/api/pedidos/$ID2" | json

echo; echo "== 6. Stock después, notificaciones, despachos y auditoría (RabbitMQ) =="
api "$INVENTARIO/api/inventario" | json
api "$NOTIF/api/notificaciones" | json
api "$INVENTARIO/api/inventario/despachos" | json
api "$NOTIF/api/notificaciones/auditoria" | json

echo; echo "== 7. Consumer groups y LAG (Kafka) =="
api "$KADMIN/api/kafka/consumer-groups" | json
for g in inventario-group pedido-group notificacion-pedidos-group notificacion-stock-group; do
  echo "-- lag de $g --"
  api "$KADMIN/api/kafka/consumer-groups/$g/lag" | json
done

echo; echo "== 8. Mensaje corrupto -> DLT de Kafka (ver también en Kafka UI: pedidos.eventos.DLT) =="
docker exec -i kafka1 bash -c "echo 'p-roto:{esto no es json' | kafka-console-producer --bootstrap-server kafka1:29092 --topic pedidos.eventos --property parse.key=true --property key.separator=:"
sleep 5
docker exec kafka1 kafka-console-consumer --bootstrap-server kafka1:29092 --topic pedidos.eventos.DLT --from-beginning --max-messages 1 --timeout-ms 10000 || true

echo; echo "== 9. Clúster RabbitMQ (2 nodos) =="
api "$RADMIN/api/rabbit/cluster" | json

echo; echo "== 10. Exchanges (direct y topic), colas (3 + 3 DLQ) y bindings =="
api "$RADMIN/api/rabbit/exchanges" | json
api "$RADMIN/api/rabbit/queues" | python3 "$(dirname "$0")/_colas.py" todas
api "$RADMIN/api/rabbit/bindings" | json

echo; echo "== 11. Mensaje defectuoso en RabbitMQ -> reintentos -> DLQ =="
curl -s -u "$RABBIT_USER:$RABBIT_PASSWORD" -H 'content-type: application/json' \
  -X POST "$RABBIT_MGMT/api/exchanges/%2F/eventos.topic/publish" \
  -d '{"properties":{"content_type":"application/json"},"routing_key":"pedido.roto","payload":"{esto no es json","payload_encoding":"string"}'
echo; echo "(esperando los reintentos: 1s + 2s ...)"; sleep 8
api "$RADMIN/api/rabbit/queues" | python3 "$(dirname "$0")/_colas.py" dlq

echo; echo "Demo terminada."
