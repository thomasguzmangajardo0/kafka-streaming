#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Crea (de forma idempotente) los tópicos del sistema con su configuración.
#
# Uso desde el host (clúster levantado con docker compose):
#   ./scripts/crear-topicos.sh
# También lo ejecuta el servicio "kafka-init" del docker-compose.
#
# Variables opcionales:
#   BOOTSTRAP        lista de brokers (por defecto, la red interna de docker)
#   KAFKA_TOPICS_CMD comando kafka-topics (por defecto: docker exec kafka1 kafka-topics)
# ---------------------------------------------------------------------------
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka1:29092,kafka2:29092,kafka3:29092}"
if [[ -z "${KAFKA_TOPICS_CMD:-}" ]]; then
  KAFKA_TOPICS_CMD="docker exec kafka1 kafka-topics"
fi

crear() {
  local nombre="$1" particiones="$2" replicas="$3"; shift 3
  echo ">> Tópico ${nombre} (particiones=${particiones}, réplicas=${replicas})"
  local cfg=()
  for c in "$@"; do cfg+=(--config "$c"); done
  ${KAFKA_TOPICS_CMD} --bootstrap-server "${BOOTSTRAP}" --create --if-not-exists \
    --topic "${nombre}" --partitions "${particiones}" --replication-factor "${replicas}" "${cfg[@]}"
}

# Eventos de pedidos: se conservan 7 días (reproceso / auditoría)
crear pedidos.eventos 3 3 retention.ms=604800000 cleanup.policy=delete min.insync.replicas=2

# Eventos de stock: compactado (queda el último resultado por pedidoId) + límite de 30 días
crear inventario.eventos 3 3 retention.ms=2592000000 cleanup.policy=compact,delete min.insync.replicas=2

# Dead Letter Topics (mensajes que no se pudieron procesar)
crear pedidos.eventos.DLT    3 3 retention.ms=2592000000 cleanup.policy=delete min.insync.replicas=2
crear inventario.eventos.DLT 3 3 retention.ms=2592000000 cleanup.policy=delete min.insync.replicas=2

echo ">> Tópicos existentes:"
${KAFKA_TOPICS_CMD} --bootstrap-server "${BOOTSTRAP}" --list
