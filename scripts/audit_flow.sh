#!/bin/bash

# ==============================================================================
# SCRIPT DE FLUXO DE AUDITORIA COMPLETO
#
# Este script automatiza todo o processo de verificação de uma chave, simulando
# um fluxo de ponta a ponta:
#
# 1. Busca a chave pública de uma chave específica (pelo ID) na API.
# 2. Exporta a chave privada criptografada (protocolo de Key Wrapping) e a salva
#    em um arquivo JSON temporário.
# 3. Executa um contêiner Docker com um ambiente Python limpo para realizar a
#    auditoria, chamando o script `run_verify.sh` que, por sua vez, executa
#    o `audit_key.py`.
# 4. Limpa os arquivos temporários.
#
# Uso: ./audit_flow.sh <id_da_chave>
# ==============================================================================

# Verifica se o ID da chave foi passado como argumento
if [ -z "$1" ]; then
    echo "Uso: $0 <id_da_chave>"
    echo "Exemplo: $0 1"
    exit 1
fi

# --- Variáveis ---
KEY_ID=$1
API_URL="http://localhost:8082/api/v1/keys"
# Obtém o diretório onde o script está localizado para garantir que os caminhos funcionem
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPORT_FILE="$SCRIPT_DIR/temp_export_${KEY_ID}.json"

echo "--- 🚀 Iniciando Auditoria Completa para a Chave de ID: $KEY_ID ---"

# 1. Buscar a Chave Pública
echo "[1/3] Buscando Chave Pública da API..."
KEYS_JSON=$(curl -s "$API_URL")

# Usa um one-liner em Python para extrair a `publicKey` do array JSON com base no ID
PUB_KEY=$(echo "$KEYS_JSON" | python3 -c "import sys, json; keys = json.load(sys.stdin); print(next((k['publicKey'] for k in keys if k['id'] == $KEY_ID), 'NOT_FOUND'))")

if [ "$PUB_KEY" == "NOT_FOUND" ]; then
    echo "[ERRO] Chave com ID $KEY_ID não encontrada no sistema."
    exit 1
fi
echo "      Chave Pública encontrada (inicia com): ${PUB_KEY:0:20}..."

# 2. Exportar a Chave Privada Criptografada
echo "[2/3] Exportando a Chave Privada Criptografada..."
curl -s -X POST "$API_URL/$KEY_ID/export" > "$EXPORT_FILE"

# Verifica se o arquivo não está vazio ou contém um erro do servidor
if grep -q "Internal Server Error" "$EXPORT_FILE" || [ ! -s "$EXPORT_FILE" ]; then
    echo "[ERRO] Falha ao exportar a chave privada."
    rm -f "$EXPORT_FILE"
    exit 1
fi
echo "      Exportação salva em arquivo temporário."

# 3. Executar o Ambiente de Auditoria no Docker
echo "[3/3] Executando Ambiente de Auditoria de Segurança..."
echo "---------------------------------------------------"

# Monta o diretório de scripts como um volume no contêiner para que ele possa
# acessar os outros scripts (run_verify.sh, audit_key.py, verify_key.py).
sudo docker run --rm \
    -v "$SCRIPT_DIR":/scripts \
    python:3.10-slim \
    /scripts/run_verify.sh "/scripts/temp_export_${KEY_ID}.json" "$PUB_KEY"

EXIT_CODE=$?

echo "---------------------------------------------------"

# Limpeza do arquivo temporário
rm -f "$EXPORT_FILE"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Processo de auditoria finalizado com sucesso."
else
    echo "❌ Processo de auditoria falhou."
fi
