import base64
from Crypto.Cipher import AES
from Crypto.PublicKey import RSA
from Crypto.Signature import pkcs1_15
from Crypto.Hash import SHA256
import hashlib
import sys

def decrypt_private_key(encrypted_private_key_b64, transport_key_b64):
    try:
        encrypted_bytes = base64.b64decode(encrypted_private_key_b64)
        transport_key = base64.b64decode(transport_key_b64)
        cipher = AES.new(transport_key, AES.MODE_ECB)
        decrypted_bytes = cipher.decrypt(encrypted_bytes)
        padding_len = decrypted_bytes[-1]
        private_key = decrypted_bytes[:-padding_len]
        return private_key.decode('utf-8')
    except Exception as e:
        return f"Error: {str(e)}"

def get_key_fingerprint(n):
    """Retorna um hash curto do módulo para comparação visual facil"""
    return hashlib.sha256(str(n).encode()).hexdigest()[:16] + "..."

def validate_key_pair(private_key_raw, public_key_b64=None):
    print("\n--- Key Audit & Validation ---")
    
    try:
        # Formatar para PEM se vier apenas o Base64 cru (comum em APIs REST)
        if "-----BEGIN" not in private_key_raw:
            private_key_pem = f"-----BEGIN PRIVATE KEY-----\n{private_key_raw}\n-----END PRIVATE KEY-----"
        else:
            private_key_pem = private_key_raw

        # 1. Carregar Chave Privada
        priv_key_obj = RSA.import_key(private_key_pem)
        print(f"[*] Key Size: {priv_key_obj.size_in_bits()} bits")
        print(f"[*] Private Key Modulus (n): {get_key_fingerprint(priv_key_obj.n)}")
        
        if not public_key_b64:
            print("[!] No Public Key provided for pair verification.")
            return

        # 2. Carregar Chave Pública fornecida
        # A API retorna X.509 SubjectPublicKeyInfo em Base64, precisamos decodificar
        pub_key_bytes = base64.b64decode(public_key_b64)
        pub_key_obj = RSA.import_key(pub_key_bytes)
        print(f"[*] Public Key Modulus (n):  {get_key_fingerprint(pub_key_obj.n)}")

        # 3. Comparação Matemática (Módulo)
        if priv_key_obj.n == pub_key_obj.n:
            print("[SUCCESS] MATCH: The Private Key and Public Key belong to the same pair.")
        else:
            print("[ERROR] MISMATCH: Modulus do not match! These keys are NOT a pair.")
            return

        # 4. Teste Funcional (Assinatura)
        print("\n[*] Performing Functional Test (Sign/Verify)...")
        message = b"Quantum Random Service - Verification Test Message"
        print(f"    1. Original Message: '{message.decode()}'")
        
        digest = SHA256.new(message)
        
        # Assinar com a Privada
        signature = pkcs1_15.new(priv_key_obj).sign(digest)
        print(f"    2. Generated Signature (Hex): {signature.hex()[:32]}... (truncated)")
        
        # Verificar com a Pública
        try:
            print(f"    3. Verifying signature using Public Key...")
            pkcs1_15.new(pub_key_obj).verify(digest, signature)
            print("[SUCCESS] Signature Verified: The Public Key successfully validated the message signed by the Private Key.")
        except (ValueError, TypeError):
            print("[ERROR] Signature Verification FAILED.")
            
    except Exception as e:
        print(f"[ERROR] Validation failed: {str(e)}")