import axios from 'axios';

/**
 * Instância pré-configurada do Axios para comunicação com a API do Key Manager.
 *
 * A baseURL é obtida de uma variável de ambiente (VITE_API_URL), com fallback
 * para o endereço local padrão, permitindo fácil configuração entre ambientes
 * de desenvolvimento e produção.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8082/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Define o corpo da requisição para criar uma nova chave RSA.
 */
export interface CreateKeyRequest {
  alias: string;
  keySize: number;
}

/**
 * Define a estrutura de dados de uma chave RSA retornada pela API.
 * Não contém a chave privada.
 */
export interface RsaKeyResponse {
  id: number;
  alias: string;
  publicKey: string;
  keySize: number;
  createdAt: string;
}

/**
 * Define a resposta da API ao exportar uma chave privada.
 * Contém a chave privada criptografada e a chave de transporte para decriptografia.
 */
export interface KeyExportResponse {
  /** A chave privada, criptografada com a chave de transporte (em Base64). */
  encryptedPrivateKey: string;
  /** A chave de transporte temporária (AES-256) para decriptografar a chave privada (em Base64). */
  transportKey: string;
  /** O algoritmo usado para a criptografia de transporte. */
  algorithm: string;
}

/**
 * Objeto de serviço que agrupa todas as chamadas de API relacionadas ao endpoint `/keys`.
 */
export const keyService = {
  /**
   * Envia uma requisição para criar uma nova chave RSA.
   * @param data Os dados da chave a ser criada (alias e keySize).
   * @returns Os metadados da chave recém-criada.
   */
  createKey: async (data: CreateKeyRequest) => {
    const response = await api.post<RsaKeyResponse>('/keys', data);
    return response.data;
  },

  /**
   * Busca a lista de todas as chaves RSA armazenadas no cofre.
   * @returns Um array com os metadados de todas as chaves.
   */
  listKeys: async () => {
    const response = await api.get<RsaKeyResponse[]>('/keys');
    return response.data;
  },

  /**
   * Inicia o processo de exportação segura de uma chave privada.
   * @param id O ID da chave a ser exportada.
   * @returns O payload de exportação contendo a chave privada criptografada e a chave de transporte.
   */
  exportKey: async (id: number) => {
    const response = await api.post<KeyExportResponse>(`/keys/${id}/export`);
    return response.data;
  },

  /**
   * Deleta uma chave específica do cofre.
   * @param id O ID da chave a ser deletada.
   */
  deleteKey: async (id: number) => {
    await api.delete(`/keys/${id}`);
  },

  /**
   * Deleta TODAS as chaves do cofre.
   */
  deleteAllKeys: async () => {
    await api.delete('/keys');
  }
};

/**
 * Define a estrutura de dados do status da entropia quântica.
 */
export interface EntropyStatus {
  /** Quantidade de unidades de entropia disponíveis para uso. */
  availableRecords: number;
  /** Custo (em unidades) para gerar uma nova chave. */
  costPerGeneration: number;
  /** Custo (em unidades) para exportar uma chave. */
  costPerExport: number;
}

/**
 * Objeto de serviço para o endpoint de status da entropia.
 */
export const entropyService = {
  /**
   * Busca o status atual do "combustível quântico" (entropia).
   * @returns O status da entropia, incluindo registros disponíveis e custos.
   */
  getStatus: async () => {
    const response = await api.get<EntropyStatus>('/quantum-entropy/status');
    return response.data;
  }
};

export default api;