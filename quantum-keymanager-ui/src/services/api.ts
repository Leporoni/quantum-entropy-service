import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 
          (window.location.port === '8000' ? '/api/v1' : 'http://localhost:8082/api/v1'),
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface CreateKeyRequest {
  alias: string;
  keySize: number;
}

export interface RsaKeyResponse {
  id: number;
  alias: string;
  publicKey: string;
  keySize: number;
  createdAt: string;
}

export interface KeyExportResponse {
  encryptedPrivateKey: string; // Base64
  transportKey: string;        // Base64
  algorithm: string;           // ex: "AES-256"
}

export const keyService = {
  createKey: async (data: CreateKeyRequest) => {
    const response = await api.post<RsaKeyResponse>('/keys', data);
    return response.data;
  },

  listKeys: async () => {
    const response = await api.get<RsaKeyResponse[]>('/keys');
    return response.data;
  },

  exportKey: async (id: number) => {
    const response = await api.post<KeyExportResponse>(`/keys/${id}/export`);
    return response.data;
  },

  deleteKey: async (id: number) => {
    await api.delete(`/keys/${id}`);
  },

  deleteAllKeys: async () => {
    await api.delete('/keys');
  }
};

export interface EntropyStatus {
  availableRecords: number;
  availableBytes: number;
  costPerGeneration: number;
  costPerExport: number;
}

export interface AuditMetrics {
  source: string;
  shannonEntropy: number;
  chiSquare: number;
  piEstimate: number;
  compressionRatio: number;
  repetitions: number;
  base64Sample: string;
}

export interface EntropyAuditReport {
  sampleSize: number;
  results: AuditMetrics[];
}

export const entropyService = {
  getStatus: async () => {
    const response = await api.get<EntropyStatus>('/quantum-entropy/status');
    return response.data;
  },

  auditEntropy: async (size: number = 2048) => {
    const response = await api.get<EntropyAuditReport>(`/quantum-entropy/audit?size=${size}`);
    return response.data;
  }
};

export default api;