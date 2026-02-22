import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Trash2, ExternalLink, Shield, Calendar, Fingerprint, Download } from 'lucide-react';
import Footer from '../components/Footer';
import { keyService, type RsaKeyResponse } from '../services/api';
import { decryptPrivateKey } from '../utils/crypto';
import './KeyVault.css';

const KeyVault: React.FC = () => {
  const [keys, setKeys] = useState<RsaKeyResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [exportingKeyId, setExportingKeyId] = useState<number | null>(null);

  const fetchKeys = async () => {
    try {
      setIsLoading(true);
      const data = await keyService.listKeys();
      setKeys(data);
    } catch (error) {
      console.error("Erro ao carregar cofre:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchKeys();
  }, []);

  const handleDelete = async (id: number, alias: string) => {
    if (window.confirm(`CONFIRM: The deletion of key "${alias}" is irreversible and will prevent access to any data encrypted by it.`)) {
      try {
        await keyService.deleteKey(id);
        setKeys(keys.filter(k => k.id !== id));
      } catch (error) {
        alert("Error deleting key.");
      }
    }
  };

  const handleClearVault = async () => {
    if (window.confirm("CRITICAL WARNING: Are you sure you want to PURGE the entire vault? This action is IRREVERSIBLE and will destroy ALL keys.")) {
      try {
        await keyService.deleteAllKeys();
        setKeys([]);
        alert("Vault purged successfully.");
      } catch (error) {
        alert("Failed to purge vault.");
      }
    }
  };

  const handleExportPrivateKey = async (id: number, alias: string) => {
    // Confirm entropy consumption
    if (!window.confirm(`Export private key for "${alias}"?\n\n⚠️ This operation will consume 2 units of quantum entropy to generate a secure transport key.`)) {
      return;
    }

    setExportingKeyId(id);
    try {
      // 1. Request encrypted private key from backend
      const exportData = await keyService.exportKey(id);

      // 2. Decrypt client-side (same as KeyManager)
      const decryptedPrivate = decryptPrivateKey(exportData.encryptedPrivateKey, exportData.transportKey);

      // 3. Download as file with readable timestamp
      const now = new Date();
      const dateStr = now.toISOString().split('T')[0]; // YYYY-MM-DD
      const timeStr = now.toTimeString().split(' ')[0].replace(/:/g, '-'); // HH-MM-SS
      const timestamp = `${dateStr}_${timeStr}`;

      const element = document.createElement("a");
      const file = new Blob([decryptedPrivate], { type: "text/plain" });
      element.href = URL.createObjectURL(file);
      element.download = `${alias}_private_${timestamp}.pem`;
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);

      alert(`Private key for "${alias}" exported successfully.`);
    } catch (error: any) {
      console.error(error);
      if (error.response?.status === 422) {
        alert("⚠️ QUANTUM FUEL DEPLETED\n\nInsufficient entropy to export key. Please wait for the collector to refuel.");
      } else {
        alert("Failed to export private key. Check console for details.");
      }
    } finally {
      setExportingKeyId(null);
    }
  };

  const handleExportPublicKey = (key: RsaKeyResponse) => {
    // No entropy consumption, direct download
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0]; // YYYY-MM-DD
    const timeStr = now.toTimeString().split(' ')[0].replace(/:/g, '-'); // HH-MM-SS
    const timestamp = `${dateStr}_${timeStr}`;

    const element = document.createElement("a");
    const file = new Blob([key.publicKey], { type: "text/plain" });
    element.href = URL.createObjectURL(file);
    element.download = `${key.alias}_public_${timestamp}.pem`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

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
