# Quantum Key Manager UI

Este é o projeto de frontend para o **Quantum Key Manager**. Desenvolvido com React, Vite e TypeScript, ele oferece uma interface de usuário moderna com um tema "Cyberpunk Corporate" para interagir com a API de gerenciamento de chaves.

## Funcionalidades

- **Dashboard Principal**: Exibe o medidor de entropia ("Quantum Fuel") e um log de operações em tempo real.
- **Geração de Chaves**: Permite criar novas chaves RSA, especificando um alias e o tamanho da chave.
- **Cofre de Chaves (Key Vault)**: Lista todas as chaves armazenadas, permitindo busca e exclusão.
- **Exportação Segura**: Implementa o fluxo de *Client-Side Key Unwrapping*. A chave privada é recebida criptografada com uma chave de transporte temporária e decifrada localmente no navegador, garantindo que a chave privada em texto plano nunca trafegue pela rede.

## Stacks e Bibliotecas

- **Framework**: React 19 com Vite e TypeScript
- **Roteamento**: React Router
- **Chamadas de API**: Axios
- **Criptografia (Client-Side)**: `node-forge` para decriptografia AES.
- **Estilização**: CSS Modules e CSS puro.
- **Ícones**: Lucide React

---

## Executando em Modo de Desenvolvimento

Para rodar a interface localmente para desenvolvimento:

1.  **Navegue até o diretório do projeto**:
    ```bash
    cd quantum-keymanager-ui
    ```

2.  **Instale as dependências**:
    ```bash
    npm install
    ```

3.  **Inicie o servidor de desenvolvimento**:
    ```bash
    npm run dev
    ```

    A aplicação estará disponível em `http://localhost:5173` (ou em outra porta, se a 5173 estiver em uso). O servidor de desenvolvimento oferece Hot Module Replacement (HMR) para uma experiência de desenvolvimento mais fluida.

## Scripts Disponíveis

- `npm run dev`: Inicia o servidor de desenvolvimento.
- `npm run build`: Compila e otimiza a aplicação para produção.
- `npm run lint`: Executa o linter (ESLint) para análise estática do código.
- `npm run preview`: Inicia um servidor local para visualizar a versão de produção (build).
