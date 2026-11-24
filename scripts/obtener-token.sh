#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Obtiene un JWT (access token) de Amazon Cognito para probar el backend / API Gateway desde la terminal.
#
#   export TOKEN=$(./scripts/obtener-token.sh usuario@correo.cl 'Clave123!')
#
# Requisitos: AWS CLI configurado y un app client con el flujo "ALLOW_USER_PASSWORD_AUTH" habilitado
# (puede ser un segundo app client solo para pruebas). Variables:
#   COGNITO_CLIENT_ID   app client id            COGNITO_REGION   región (por defecto us-east-1)
# ---------------------------------------------------------------------------
set -euo pipefail
USUARIO="${1:?uso: $0 <usuario> <clave>}"
CLAVE="${2:?uso: $0 <usuario> <clave>}"
: "${COGNITO_CLIENT_ID:?defina COGNITO_CLIENT_ID}"

aws cognito-idp initiate-auth \
  --region "${COGNITO_REGION:-us-east-1}" \
  --auth-flow USER_PASSWORD_AUTH \
  --client-id "$COGNITO_CLIENT_ID" \
  --auth-parameters "USERNAME=${USUARIO},PASSWORD=${CLAVE}" \
  --query 'AuthenticationResult.AccessToken' --output text
