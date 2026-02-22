# Quantum Entropy Service

A microservices project that fetches quantum random numbers from an external API, exposes them via a Spring Boot service, and utilizes them to generate secure RSA keys via a dedicated Key Manager — all managed through a modern web interface.

## Architecture Overview

The system consists of three main components running in Docker containers:

1. **Quantum API (Java/Spring Boot)**: Fetches random numbers (via HEX format) and exposes them as Base64 strings.
2. **Quantum Key Manager (Java/Spring Boot)**: Consumes quantum entropy to generate and manage RSA keys securely. Uses an in-memory H2 database.
3. **Quantum Key Manager UI (React/Vite)**: Modern "Cyberpunk Corporate" interface for secure key generation, visualization, and export.

## Prerequisites

- **Docker** and **Docker Compose** installed on your machine.
- (Optional) Java 21 and Maven if you want to run the API locally without Docker.

---

## 🚀 Running with Docker (Recommended)

This is the easiest way to run the entire system.

1. **Build and Start the services**:
    ```bash
    docker compose up --build -d
    ```
    This command will build the Java APIs and the React UI images, and start everything in the background.

2. **Check Status**:
    To see if everything is running:
    ```bash
    docker compose ps
    ```

3. **View Logs**:
    - **Quantum API**:
      ```bash
      docker compose logs -f quantum-service
      ```
    - **Key Manager**:
      ```bash
      docker compose logs -f quantum-keymanager
      ```
    - **Frontend UI**:
      ```bash
      docker compose logs -f quantum-keymanager-ui
      ```

4. **Stop the services**:
    ```bash
    docker compose down
    ```

---

## 🎨 Quantum Key Manager UI

The web interface runs on port **3000** and provides a high-end dashboard to manage your quantum keys.

- **URL**: [http://localhost:3000](http://localhost:3000)
- **Features**:
    - **Entropy Meter**: Real-time visualization of available entropy in the system.
    - **Real-time Logs**: Terminal simulation for quantum operations.
    - **Secure Client-Side Decryption**: Keys are unwrapped in the browser for maximum security.

---

## 🔑 Quantum Key Manager API

The `quantum-keymanager` service runs on port **8082** and provides endpoints to generate and export RSA keys using quantum entropy.

### 1. Generate a New RSA Key
Generates a key pair (Public/Private), encrypts the private key with a system Master Key, and stores it. It consumes quantum entropy to seed the generator.

```bash
curl -X POST http://localhost:8082/api/v1/keys \
  -H "Content-Type: application/json" \
  -d '{"alias": "my-secure-key", "keySize": 2048}'
```

### 2. List All Keys
Returns the list of stored keys (ID, Alias, Public Key, and Creation Date).

```bash
curl -X GET http://localhost:8082/api/v1/keys
```

### 3. Securely Export a Private Key
Uses **Key Wrapping**: consumes fresh quantum entropy to create a temporary Transport Key (AES-256), which encrypts the requested Private Key for safe transport.

```bash
# Replace {id} with the actual Key ID obtained from the list command
curl -X POST http://localhost:8082/api/v1/keys/1/export
```

---

## 🔐 Security Verification (Testing the Export)

To verify that the exported private key is valid and can be decrypted by the recipient, you can use the provided Python verification scripts or the **Frontend UI**. This process confirms the **Key Wrapping** logic works as expected.

### 1. Requirements
You don't need to install Python or dependencies locally if you have Docker. The verification scripts are located in `scripts/`.

### 2. Run Automated Verification (Recommended)
We provide an automated script that fetches the public key from the API, exports the private key, and runs the audit inside a Docker container:

```bash
# Run from the project root
./scripts/audit_flow.sh <key_id>

# Example for Key ID 1:
./scripts/audit_flow.sh 1
```

---

## Project Structure

- `src/`: Java Spring Boot source code (Quantum API).
- `quantum-keymanager/`: Java Spring Boot source code (Key Manager).
- `quantum-keymanager-ui/`: React/Vite source code (Frontend).
- `docker-compose.yml`: Orchestration for all services.
- `render.yaml`: Render.com deployment configuration.
- `scripts/`: Python scripts for security auditing.

## 👨‍💻 Developed by
**Leporoni Tech Solutions**  
📧 [leporonitech@gmail.com](mailto:leporonitech@gmail.com)

---
