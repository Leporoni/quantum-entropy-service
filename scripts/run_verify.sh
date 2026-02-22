#!/bin/bash
# Script wrapper para rodar dentro do Docker
# Instala dependências e executa o audit_key.py com os argumentos recebidos

pip install pycryptodome > /dev/null 2>&1

# O primeiro argumento ($1) é o arquivo JSON (caminho dentro do container)
# O segundo argumento ($2) é a chave pública
python /scripts/audit_key.py "$1" "$2"