import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Trash2, ExternalLink, Shield, Calendar, Fingerprint, Download } from 'lucide-react';
import Footer from '../components/Footer';
import { keyService, type RsaKeyResponse } from '../services/api';
import { decryptPrivateKey } from '../utils/crypto';
import './KeyVault.css';

/**
 * Componente da página "Cofre de Chaves" (Key Vault).
 *
 * Esta página é responsável por exibir, gerenciar e permitir a exportação
 * de todas as chaves RSA armazenadas no sistema.
 * Funcionalidades:
 * - Listagem de todas as chaves com seus metadados.
 * - Filtro/busca por alias ou ID.
 * - Exclusão de chaves individuais.
 * - Limpeza completa do cofre (exclusão de todas as chaves).
 * - Exportação segura da chave privada, com decriptografia no lado do cliente.
 */
const KeyVault: React.FC = () => {
  // --- STATE MANAGEMENT ---

  // Lista de chaves RSA buscadas da API.
  const [keys, setKeys] = useState<RsaKeyResponse[]>([]);
  // Controla a exibição de um estado de "carregando" enquanto as chaves são buscadas.
  const [isLoading, setIsLoading] = useState(true);
  // Termo de busca inserido pelo usuário para filtrar as chaves.
  const [searchTerm, setSearchTerm] = useState('');
  // Armazena o ID da chave que está sendo exportada para desabilitar o botão e mostrar feedback.
  const [exportingKeyId, setExportingKeyId] = useState<number | null>(null);

  /**
   * Busca a lista de chaves da API e atualiza o estado do componente.
   */
  const fetchKeys = async () => {
    try {
      setIsLoading(true);
      const data = await keyService.listKeys();
      setKeys(data);
    } catch (error) {
      console.error("Erro ao carregar cofre:", error);
      alert("Falha ao carregar as chaves do cofre.");
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Efeito que busca as chaves da API assim que o componente é montado.
   */
  useEffect(() => {
    fetchKeys();
  }, []);

  /**
   * Manipula a exclusão de uma chave específica.
   * Pede confirmação ao usuário antes de prosseguir.
   * @param id O ID da chave a ser deletada.
   * @param alias O alias da chave, para exibição na confirmação.
   */
  const handleDelete = async (id: number, alias: string) => {
    if (window.confirm(`CONFIRMAR: A exclusão da chave "${alias}" é irreversível. Deseja continuar?`)) {
      try {
        await keyService.deleteKey(id);
        // Remove a chave da lista local para atualizar a UI instantaneamente.
        setKeys(keys.filter(k => k.id !== id));
      } catch (error) {
        alert("Erro ao deletar a chave.");
      }
    }
  };

  /**
   * Manipula a exclusão de TODAS as chaves do cofre.
   * Exibe um aviso crítico e pede dupla confirmação.
   */
  const handleClearVault = async () => {
    if (window.confirm("ALERTA CRÍTICO: Tem certeza que deseja limpar TODO o cofre? Esta ação é IRREVERSÍVEL e destruirá TODAS as chaves.")) {
      try {
        await keyService.deleteAllKeys();
        setKeys([]);
        alert("Cofre limpo com sucesso.");
      } catch (error) {
        alert("Falha ao limpar o cofre.");
      }
    }
  };

  /**
   * Orquestra a exportação segura da chave privada.
   * @param id O ID da chave a ser exportada.
   * @param alias O alias da chave, para nomear o arquivo.
   */
  const handleExportPrivateKey = async (id: number, alias: string) => {
    if (!window.confirm(`Exportar a chave privada para "${alias}"?\n\n⚠️ Esta operação consumirá 2 unidades de entropia quântica.`)) {
      return;
    }

    setExportingKeyId(id);
    try {
      // 1. Requisita a chave privada criptografada do backend (protocolo de Key Wrapping)
      const exportData = await keyService.exportKey(id);

      // 2. Decriptografa a chave no lado do cliente
      const decryptedPrivate = decryptPrivateKey(exportData.encryptedPrivateKey, exportData.transportKey);

      // 3. Inicia o download do arquivo
      downloadFile(decryptedPrivate, `${alias}_private_key.pem`);

      alert(`Chave privada para "${alias}" exportada com sucesso.`);
    } catch (error: any) {
      console.error(error);
      if (error.response?.status === 422) {
        alert("⚠️ COMBUSTÍVEL QUÂNTICO INSUFICIENTE\n\nNão há entropia para exportar a chave. Aguarde o reabastecimento.");
      } else {
        alert("Falha ao exportar a chave privada. Verifique o console.");
      }
    } finally {
      setExportingKeyId(null);
    }
  };

  /**
   * Inicia o download da chave pública (operação simples, sem custo de entropia).
   * @param key O objeto da chave contendo a chave pública.
   */
  const handleExportPublicKey = (key: RsaKeyResponse) => {
    downloadFile(key.publicKey, `${key.alias}_public_key.pem`);
  };

  /**
   * Função utilitária para criar e acionar o download de um arquivo de texto.
   * @param content O conteúdo do arquivo.
   * @param fileName O nome do arquivo.
   */
  const downloadFile = (content: string, fileName: string) => {
    const element = document.createElement("a");
    const file = new Blob([content], { type: "text/plain" });
    element.href = URL.createObjectURL(file);
    element.download = fileName;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }

  // Deriva a lista de chaves filtradas a partir do estado `keys` e `searchTerm`.
  const filteredKeys = keys.filter(k =>
    k.alias.toLowerCase().includes(searchTerm.toLowerCase()) ||
    k.id.toString().includes(searchTerm)
  );

  return (
    <div className="vault-container">
      <header className="vault-header">
        <div className="brand-small">CRYPTO <span>QUANTUM</span> VAULT</div>
        <Link to="/" className="back-link">
          <ArrowLeft size={16} /> Dashboard
        </Link>
      </header>

      <main className="vault-content">
        <div className="vault-intro">
          <h1><Shield size={32} color="var(--accent-cyan)" /> Key Management Vault</h1>
          <p>Manage your inventory of RSA keys generated with quantum entropy.</p>
        </div>

        <div className="vault-controls">
          <div className="search-box">
            <input
              type="text"
              placeholder="Filter by Alias or ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <div className="controls-actions">
            <button className="btn-refresh" onClick={fetchKeys}>Refresh Vault</button>
            <button className="btn-clear-vault" onClick={handleClearVault}>
              <Trash2 size={16} /> Clear Vault
            </button>
          </div>
        </div>

        {isLoading ? (
          <div className="vault-loading">Scanning database sectors...</div>
        ) : (
          <div className="keys-grid">
            {filteredKeys.length === 0 ? (
              keys.length === 0 ? (
                <div className="empty-vault">
                  <p>The vault is currently empty.</p>
                  <Link to="/keymanager" className="btn-empty-action">
                    <ExternalLink size={16} /> Access Generator
                  </Link>
                </div>
              ) : (
                <div className="empty-vault">No keys found in the selected sector.</div>
              )
            ) : (
              filteredKeys.map(key => (
                <div key={key.id} className="key-card">
                  <div className="key-card-header">
                    <Fingerprint size={20} color="var(--accent-cyan)" />
                    <span className="key-id">ID: {key.id}</span>
                  </div>

                  <div className="key-alias">{key.alias}</div>

                  <div className="key-info">
                    <div className="info-item">
                      <Calendar size={14} />
                      <span>{new Date(key.createdAt).toLocaleDateString()}</span>
                    </div>
                    <div className="info-item">
                      <Shield size={14} />
                      <span>RSA-{key.keySize}</span>
                    </div>
                  </div>

                  <div className="key-actions">
                    <Link to="/keymanager" className="action-btn export">
                      <ExternalLink size={16} /> Generator
                    </Link>
                    <button
                      className="action-btn export"
                      onClick={() => handleExportPublicKey(key)}
                    >
                      <Download size={16} /> Export Public
                    </button>
                    <button
                      className="action-btn export"
                      onClick={() => handleExportPrivateKey(key.id, key.alias)}
                      disabled={exportingKeyId === key.id}
                    >
                      <Download size={16} /> {exportingKeyId === key.id ? 'Exporting...' : 'Export Private'}
                    </button>
                    <button
                      className="action-btn delete"
                      onClick={() => handleDelete(key.id, key.alias)}
                    >
                      <Trash2 size={16} /> Delete
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};

export default KeyVault;
