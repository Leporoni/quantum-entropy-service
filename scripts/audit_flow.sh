#!/bin/bash

# Verifica se o ID foi passado
if [ -z "$1" ]; then
    echo "Usage: $0 <key_id>"
    echo "Example: $0 1"
    exit 1
fi

KEY_ID=$1
API_URL="http://localhost:8082/api/v1/keys"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPORT_FILE="$SCRIPT_DIR/temp_export_${KEY_ID}.json"

echo "--- 🚀 Starting Full Audit for Key ID: $KEY_ID ---"

# 1. Buscar a Chave Pública
echo "[1/3] Fetching Public Key from API..."
KEYS_JSON=$(curl -s "$API_URL")

# Usar Python one-liner para extrair a publicKey do JSON array baseada no ID
PUB_KEY=$(echo "$KEYS_JSON" | python3 -c "import sys, json; keys = json.load(sys.stdin); print(next((k['publicKey'] for k in keys if k['id'] == $KEY_ID), 'NOT_FOUND'))")

if [ "$PUB_KEY" == "NOT_FOUND" ]; then
    echo "[ERROR] Key ID $KEY_ID not found in the system."
    exit 1
fi
echo "      Found Public Key (starts with): ${PUB_KEY:0:20}..."

# 2. Exportar a Chave Privada
echo "[2/3] Exporting Encrypted Private Key..."
curl -s -X POST "$API_URL/$KEY_ID/export" > "$EXPORT_FILE"

# Verifica se o arquivo não está vazio ou com erro
if grep -q "Internal Server Error" "$EXPORT_FILE" || [ ! -s "$EXPORT_FILE" ]; then
    echo "[ERROR] Failed to export private key."
    rm -f "$EXPORT_FILE"
    exit 1
fi
echo "      Export saved to temporary file."

# 3. Executar Auditoria no Docker
echo "[3/3] Running Security Audit Environment..."
echo "---------------------------------------------------"

# Montamos o diretório de scripts como volume
sudo docker run --rm \
    -v "$SCRIPT_DIR":/scripts \
    python:3.10-slim \
    /scripts/run_verify.sh "/scripts/temp_export_${KEY_ID}.json" "$PUB_KEY"

EXIT_CODE=$?

echo "---------------------------------------------------"
# Limpeza
rm -f "$EXPORT_FILE"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Audit process finished successfully."
else
    echo "❌ Audit process failed."
fi
