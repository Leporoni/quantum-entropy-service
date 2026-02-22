# -*- coding: utf-8 -*-
"""
Este módulo é um kit de ferramentas para verificação criptográfica.

Ele fornece funções para descriptografar uma chave privada (usando uma chave de transporte AES)
e para validar que um par de chaves (pública e privada) é matematicamente e
funcionalmente correto. É o núcleo da lógica de auditoria de segurança do sistema.
"""

import base64
from Crypto.Cipher import AES
from Crypto.PublicKey import RSA
from Crypto.Signature import pkcs1_15
from Crypto.Hash import SHA256
import hashlib
import sys

def decrypt_private_key(encrypted_private_key_b64, transport_key_b64):
    """
    Descriptografa uma chave privada usando uma chave de transporte AES.

    Esta função replica o processo de "unwrapping" que o frontend faz,
    sendo essencial para a auditoria do protocolo de exportação segura.

    Args:
        encrypted_private_key_b64 (str): A chave privada criptografada, em Base64.
        transport_key_b64 (str): A chave de transporte AES, em Base64.

    Returns:
        str: A chave privada em formato PEM, ou uma mensagem de erro.
    """
    try:
        encrypted_bytes = base64.b64decode(encrypted_private_key_b64)
        transport_key_bytes = base64.b64decode(transport_key_b64)
        
        # O Java usa AES/ECB/PKCS5Padding por padrão com "AES".
        # PyCryptodome requer que o unpadding seja feito manualmente para ECB.
        cipher = AES.new(transport_key_bytes, AES.MODE_ECB)
        decrypted_padded_bytes = cipher.decrypt(encrypted_bytes)
        
        # Remoção manual do padding PKCS5/PKCS7
        padding_len = decrypted_padded_bytes[-1]
        private_key_bytes = decrypted_padded_bytes[:-padding_len]
        
        return private_key_bytes.decode('utf-8')
    except Exception as e:
        return f"Erro na descriptografia: {str(e)}"

def get_key_fingerprint(n):
    """
    Gera uma "impressão digital" curta do módulo (n) de uma chave.
    Útil para comparação visual rápida.

    Args:
        n (int): O componente do módulo de uma chave RSA.

    Returns:
        str: Um trecho do hash SHA-256 do módulo.
    """
    return hashlib.sha256(str(n).encode()).hexdigest()[:16] + "..."

def validate_key_pair(private_key_raw, public_key_b64=None):
    """
    Valida um par de chaves RSA através de duas etapas:
    1. Verificação Matemática: Compara o módulo (n) de ambas as chaves. Se forem iguais,
       elas pertencem ao mesmo par.
    2. Teste Funcional: Assina uma mensagem com a chave privada e verifica a assinatura
       com a chave pública.

    Args:
        private_key_raw (str): A chave privada em formato PEM ou Base64 puro.
        public_key_b64 (str, optional): A chave pública em Base64. Defaults to None.
    """
    print("\n--- Auditoria e Validação de Chave ---")
    
    try:
        # Formata a chave privada para PEM se ela vier como Base64 cru.
        if "-----BEGIN" not in private_key_raw:
            private_key_pem = f"-----BEGIN PRIVATE KEY-----\n{private_key_raw}\n-----END PRIVATE KEY-----"
        else:
            private_key_pem = private_key_raw

        # 1. Carregar Chave Privada
        priv_key_obj = RSA.import_key(private_key_pem)
        print(f"[*] Tamanho da Chave: {priv_key_obj.size_in_bits()} bits")
        print(f"[*] Módulo da Chave Privada (n): {get_key_fingerprint(priv_key_obj.n)}")
        
        if not public_key_b64:
            print("[!] Nenhuma chave pública fornecida para verificação do par.")
            return

        # 2. Carregar Chave Pública
        pub_key_bytes = base64.b64decode(public_key_b64)
        pub_key_obj = RSA.import_key(pub_key_bytes)
        print(f"[*] Módulo da Chave Pública (n):  {get_key_fingerprint(pub_key_obj.n)}")

        # 3. Comparação Matemática (Módulo)
        if priv_key_obj.n == pub_key_obj.n:
            print("[SUCESSO] COMPATIBILIDADE: A Chave Privada e a Chave Pública pertencem ao mesmo par.")
        else:
            print("[ERRO] INCOMPATIBILIDADE: Os módulos não correspondem! As chaves NÃO são um par.")
            return

        # 4. Teste Funcional (Assinatura e Verificação)
        print("\n[*] Executando Teste Funcional (Assinar/Verificar)...")
        message = b"Quantum Random Service - Mensagem de Teste para Verificacao"
        print(f"    1. Mensagem Original: '{message.decode()}'")
        
        digest = SHA256.new(message)
        
        # Assinar com a chave privada
        signature = pkcs1_15.new(priv_key_obj).sign(digest)
        print(f"    2. Assinatura Gerada (Hex): {signature.hex()[:32]}... (truncado)")
        
        # Verificar com a chave pública
        try:
            print(f"    3. Verificando assinatura com a Chave Pública...")
            pkcs1_15.new(pub_key_obj).verify(digest, signature)
            print("[SUCESSO] Assinatura Verificada: A Chave Pública validou a mensagem assinada pela Chave Privada.")
        except (ValueError, TypeError):
            print("[ERRO] A verificação da assinatura FALHOU.")
            
    except Exception as e:
        print(f"[ERRO] A validação falhou: {str(e)}")