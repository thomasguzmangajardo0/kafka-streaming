"""Formatea la lista de colas que entrega GET /api/rabbit/queues (uso interno de demo.sh)."""
import json
import sys

modo = sys.argv[1] if len(sys.argv) > 1 else "todas"
for q in json.load(sys.stdin):
    if modo == "dlq":
        if q["name"].endswith(".dlq"):
            print(f'{q["name"]:<36} mensajes en DLQ = {q.get("messages", 0)}')
    else:
        print(f'{q["name"]:<36} mensajes={q.get("messages", 0):<3} consumidores={q.get("consumers", 0)} '
              f'nodo={q.get("node")} réplicas={q.get("slave_nodes")}')
