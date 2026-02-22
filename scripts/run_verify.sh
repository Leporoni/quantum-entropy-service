#!/bin/bash
# ==============================================================================
# SCRIPT DE EXECUÇÃO DENTRO DO DOCKER
#
# Este script é o ponto de entrada (entrypoint) para o contêiner de auditoria.
# Suas responsabilidades são:
# 1. Instalar as dependências Python necessárias (pycryptodome) em silêncio.
# 2. Executar o script principal de auditoria (`audit_key.py`), passando
#    adiante os argumentos que este script recebeu (caminho do arquivo JSON
#    e a string da chave pública).
# ==============================================================================

# Instala a dependência criptográfica, descartando a saída para manter o log limpo.
pip install pycryptodome > /dev/null 2>&1

echo "[*] Ambiente de auditoria pronto. Executando script de verificação..."

# O primeiro argumento () é o caminho do arquivo JSON dentro do contêiner.
# O segundo argumento ($2) é a string da chave pública.
python /scripts/audit_key.py "" "$2"