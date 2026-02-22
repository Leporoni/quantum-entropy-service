import json
import sys
import verify_key

def main():
    if len(sys.argv) < 3:
        print("Usage: python audit_key.py <json_file_path> <public_key_string>")
        print("Example: python audit_key.py key_export.json \"MIIBIjAN...\"")
        sys.exit(1)

    json_file_path = sys.argv[1]
    public_key_string = sys.argv[2]

    try:
        # Ler e parsear o JSON de exportação
        with open(json_file_path, 'r') as f:
            data = json.load(f)
        
        # Validar estrutura do JSON
        if 'encryptedPrivateKey' not in data or 'transportKey' not in data:
            print("[ERROR] Invalid JSON format. Missing 'encryptedPrivateKey' or 'transportKey'.")
            sys.exit(1)

        enc_priv_key = data['encryptedPrivateKey']
        transport_key = data['transportKey']

        print(f"[*] Loaded export data from: {json_file_path}")
        print(f"[*] Algorithm: {data.get('algorithm', 'Unknown')}")
        print("--- Initiating Audit ---")

        # 1. Descriptografar
        decrypted_pem = verify_key.decrypt_private_key(enc_priv_key, transport_key)

        if decrypted_pem.startswith("Error"):
            print(f"[ERROR] Decryption failed: {decrypted_pem}")
            sys.exit(1)
        
        print("[V] Decryption Successful (Transport Layer Verified).")

        # 2. Validar
        verify_key.validate_key_pair(decrypted_pem, public_key_string)

    except FileNotFoundError:
        print(f"[ERROR] File not found: {json_file_path}")
    except json.JSONDecodeError:
        print(f"[ERROR] Failed to parse JSON from file: {json_file_path}")
    except Exception as e:
        print(f"[ERROR] Unexpected error: {str(e)}")

if __name__ == "__main__":
    main()
