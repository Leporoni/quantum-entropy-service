# Quantum Entropy Service

Um projeto de microsserviços que busca números quânticos aleatórios de uma API externa, os expõe através de um serviço Spring Boot e os utiliza para gerar chaves RSA seguras por meio de um Key Manager dedicado — tudo gerenciado por uma interface web moderna.

## Arquitetura Geral

O sistema consiste em três componentes principais, executados em contêineres Docker:

1.  **Quantum API (Java/Spring Boot)**: Busca números aleatórios (em formato HEX) e os expõe como strings Base64.
2.  **Quantum Key Manager (Java/Spring Boot)**: Consome a entropia quântica para gerar e gerenciar chaves RSA de forma segura. Utiliza um banco de dados em memória (H2).
3.  **Quantum Key Manager UI (React/Vite)**: Uma interface moderna com estilo "Cyberpunk Corporate" para geração, visualização e exportação segura de chaves.

## Pré-requisitos

- **Docker** e **Docker Compose** instalados em sua máquina.
- (Opcional) Java 21 e Maven, caso queira executar a API localmente sem Docker.

---

## 🚀 Executando com Docker (Recomendado)

Esta é a maneira mais fácil de executar o sistema completo.

1.  **Construa e Inicie os serviços**:
    ```bash
    docker compose up --build -d
    ```
    Este comando irá construir as imagens das APIs Java e da UI em React, e iniciará todos os serviços em segundo plano.

2.  **Verifique o Status**:
    Para ver se tudo está em execução:
    ```bash
    docker compose ps
    ```

3.  **Visualize os Logs**:
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

4.  **Pare os serviços**:
    ```bash
    docker compose down
    ```

---

## 🎨 Quantum Key Manager UI

A interface web é executada na porta **3000** e oferece um dashboard de alta qualidade para gerenciar suas chaves quânticas.

- **URL**: [http://localhost:3000](http://localhost:3000)
- **Funcionalidades**:
    - **Medidor de Entropia**: Visualização em tempo real da entropia disponível no sistema.
    - **Logs em Tempo Real**: Simulação de terminal para as operações quânticas.
    - **Decriptografia Segura no Cliente**: As chaves são desembrulhadas (unwrapped) no navegador para máxima segurança.

---

## 🔑 Quantum Key Manager API

O serviço `quantum-keymanager` é executado na porta **8082** e fornece endpoints para gerar e exportar chaves RSA usando entropia quântica.

### 1. Gerar uma Nova Chave RSA
Gera um par de chaves (Pública/Privada), criptografa a chave privada com uma Master Key do sistema e a armazena. Consome entropia quântica para alimentar o gerador.

```bash
curl -X POST http://localhost:8082/api/v1/keys \
  -H "Content-Type: application/json" \
  -d '{"alias": "minha-chave-segura", "keySize": 2048}'
```

### 2. Listar Todas as Chaves
Retorna a lista de chaves armazenadas (ID, Alias, Chave Pública e Data de Criação).

```bash
curl -X GET http://localhost:8082/api/v1/keys
```

### 3. Exportar uma Chave Privada com Segurança
Utiliza **Key Wrapping**: consome nova entropia quântica para criar uma Chave de Transporte temporária (AES-256), que criptografa a Chave Privada solicitada para um transporte seguro.

```bash
# Substitua {id} pelo ID da chave obtido no comando de listagem
curl -X POST http://localhost:8082/api/v1/keys/1/export
```

---

## 🔐 Verificação de Segurança (Testando a Exportação)

Para verificar se a chave privada exportada é válida e pode ser decriptografada pelo destinatário, você pode usar os scripts de verificação em Python fornecidos ou a própria **Interface Web**. Este processo confirma que a lógica de **Key Wrapping** funciona como esperado.

### 1. Requisitos
Você não precisa instalar Python ou dependências localmente se tiver o Docker. Os scripts de verificação estão localizados em `scripts/`.

### 2. Executar Verificação Automatizada (Recomendado)
Nós fornecemos um script automatizado que busca a chave pública da API, exporta a chave privada e executa a auditoria dentro de um contêiner Docker:

```bash
# Execute a partir da raiz do projeto
./scripts/audit_flow.sh <key_id>

# Exemplo para a Chave de ID 1:
./scripts/audit_flow.sh 1
```

---

## Estrutura do Projeto

- `src/`: Código-fonte do Spring Boot (Quantum API).
- `quantum-keymanager/`: Código-fonte do Spring Boot (Key Manager).
- `quantum-keymanager-ui/`: Código-fonte do React/Vite (Frontend).
- `docker-compose.yml`: Orquestração de todos os serviços.
- `render.yaml`: Configuração de deploy para o Render.com.
- `scripts/`: Scripts em Python para auditoria de segurança.

## 👨‍💻 Desenvolvido por
**Leporoni Tech Solutions**  
📧 [leporonitech@gmail.com](mailto:leporonitech@gmail.com)

---
