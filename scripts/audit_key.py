# -*- coding: utf-8 -*-
"""
Este script é um executor de auditoria de linha de comando.

Ele simula o processo de um terceiro que recebe um payload de chave exportada
(em formato JSON) e uma chave pública, e usa o kit de ferramentas `verify_key`
para confirmar a integridade e funcionalidade do par de chaves.
"""

import json
import sys
import verify_key

def main():
    """
    Função principal que orquestra o processo de auditoria.

    Etapas:
    1. Parseia os argumentos da linha de comando (caminho do JSON, chave pública).
    2. Lê o arquivo JSON contendo a chave privada criptografada e a chave de transporte.
    3. Chama `verify_key.decrypt_private_key` para descriptografar o payload.
    4. Chama `verify_key.validate_key_pair` para realizar a validação matemática e funcional.
    """
    if len(sys.argv) < 3:
        print("Uso: python audit_key.py <caminho_do_json> <string_chave_publica>")
        print("Exemplo: python audit_key.py chave_exportada.json \"MIIBIjAN...\"")
        sys.exit(1)

    json_file_path = sys.argv[1]
    public_key_string = sys.argv[2]

    try:
        # Ler e parsear o JSON de exportação
        with open(json_file_path, 'r') as f:
            data = json.load(f)
        
        # Validar estrutura do JSON
        if 'encryptedPrivateKey' not in data or 'transportKey' not in data:
            print("[ERRO] Formato de JSON inválido. Faltando 'encryptedPrivateKey' ou 'transportKey'.")
            sys.exit(1)

        enc_priv_key = data['encryptedPrivateKey']
        transport_key = data['transportKey']

        print(f"[*] Dados de exportação carregados de: {json_file_path}")
        print(f"[*] Algoritmo: {data.get('algorithm', 'Desconhecido')}")
        print("--- Iniciando Auditoria ---")

        # 1. Descriptografar a chave privada usando a chave de transporte
        decrypted_pem = verify_key.decrypt_private_key(enc_priv_key, transport_key)

        if decrypted_pem.startswith("Erro"):
            print(f"[ERRO] Falha na descriptografia: {decrypted_pem}")
            sys.exit(1)
        
        print("[V] Descriptografia bem-sucedida (Camada de Transporte Verificada).")

        # 2. Validar o par de chaves (privada descriptografada vs. pública original)
        verify_key.validate_key_pair(decrypted_pem, public_key_string)

    except FileNotFoundError:
        print(f"[ERRO] Arquivo não encontrado: {json_file_path}")
    except json.JSONDecodeError:
        print(f"[ERRO] Falha ao parsear JSON do arquivo: {json_file_path}")
    except Exception as e:
        print(f"[ERRO] Erro inesperado: {str(e)}")

if __name__ == "__main__":
    main()
