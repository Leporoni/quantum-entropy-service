import React, { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Download, ShieldCheck, Cpu, Eye, EyeOff } from 'lucide-react';
import Footer from '../components/Footer';
import { keyService } from '../services/api';
import { decryptPrivateKey, formatPublicKey } from '../utils/crypto';
import EntropyMeter from '../components/EntropyMeter';
import './KeyManager.css';

/**
 * Componente principal da página de Gerenciamento de Chaves (Key Manager).
 *
 * Esta página funciona como a área de trabalho para gerar, visualizar e exportar
 * pares de chaves RSA. Ela orquestra a interação do usuário com os serviços de backend,
 * implementa o protocolo de decriptografia segura no lado do cliente (Client-Side Decryption)
 * e exibe logs detalhados de todo o processo.
 */
const KeyManager: React.FC = () => {
  // --- STATE MANAGEMENT ---

  // Configurações da chave a ser gerada
  const [keySize, setKeySize] = useState('2048');
  const [format, setFormat] = useState('pem');
  const [keyAlias, setKeyAlias] = useState(`key_${Date.now()}`);

  // Estado da UI
  const [isGenerating, setIsGenerating] = useState(false);
  const [logs, setLogs] = useState<string[]>(['> System ready. Connected to Quantum Backend.']);
  const [keysVisible, setKeysVisible] = useState(false);
  const [showPrivateKey, setShowPrivateKey] = useState(false);

  // Armazenamento das chaves geradas
  const [publicKey, setPublicKey] = useState('Waiting for generation...');
  const [privateKey, setPrivateKey] = useState('Waiting for generation...');

  // Referência para o elemento do terminal para auto-scroll
  const terminalRef = useRef<HTMLDivElement>(null);

  /**
   * Efeito que rola o terminal de logs para o final sempre que uma nova mensagem é adicionada.
   */
  useEffect(() => {
    if (terminalRef.current) {
      terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
    }
  }, [logs]);

  /**
   * Adiciona uma nova mensagem ao estado de logs do terminal.
   * @param message A mensagem a ser adicionada.
   */
  const addLog = (message: string) => {
    setLogs(prev => [...prev, `> ${message}`]);
  };

  /**
   * Orquestra todo o processo de geração e obtenção de um par de chaves RSA.
   * Este é o fluxo principal da aplicação.
   */
  const generateKeys = async () => {
    // 1. Reset e preparação do estado da UI
    setIsGenerating(true);
    setKeysVisible(false);
    setLogs(['> Initializing secure request...']);
    setPublicKey('Waiting for generation...');
    setPrivateKey('Waiting for generation...');

    try {
      // 2. Geração da Chave no Servidor (usando Semente Quântica)
      addLog(`Requesting RSA generation on server...`);
      addLog(`Parameters: ${keySize} bits, Alias: ${keyAlias}`);
      const createdKey = await keyService.createKey({
        alias: keyAlias,
        keySize: parseInt(keySize)
      });
      addLog(`✔ Key generated successfully! ID: ${createdKey.id}`);
      addLog("Receiving Public Key...");

      const formattedPublic = formatPublicKey(createdKey.publicKey);
      setPublicKey(formattedPublic);

      // 3. Exportação Segura da Chave Privada (Protocolo de Key Wrapping)
      addLog("Initiating Secure Export Protocol...");
      addLog("Requesting encrypted private key...");
      const exportData = await keyService.exportKey(createdKey.id);
      addLog("✔ Encrypted payload received.");
      addLog(`Transport Key (AES-256): Received via secure channel.`);

      // 4. Decriptografia no Cliente (Client-Side Decryption)
      addLog("Decrypting payload in browser (Client-Side)...");
      const decryptedPrivate = decryptPrivateKey(exportData.encryptedPrivateKey, exportData.transportKey);
      setPrivateKey(decryptedPrivate);
      addLog("✔ Decryption completed successfully.");

      // 5. Exibição das chaves
      setKeysVisible(true);

    } catch (error: any) {
      console.error(error);
      // Tratamento de erro específico para falta de entropia
      if (error.response?.status === 422) {
        const serverMessage = error.response?.data?.message || "Insufficient quantum entropy.";
        addLog("⚠️ QUANTUM FUEL DEPLETED");
        addLog(serverMessage);
        addLog("Solution: Wait for the entropy collector to refuel (check Quantum Fuel meter).");
      } else {
        addLog("❌ CRITICAL ERROR: Communication or encryption failed.");
        addLog(String(error));
      }
    } finally {
      setIsGenerating(false);
      // Atualiza o alias para a próxima geração, evitando nomes duplicados.
      setKeyAlias(`key_${Date.now()}`);
    }
  };

  /**
   * Inicia o download de um arquivo de texto contendo a chave pública ou privada.
   * @param type O tipo de chave ('public' ou 'private').
   * @param content O conteúdo da chave a ser salvo no arquivo.
   */
  const downloadKey = (type: 'public' | 'private', content: string) => {
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0];
    const timeStr = now.toTimeString().split(' ')[0].replace(/:/g, '-');
    const timestamp = `${dateStr}_${timeStr}`;

    const element = document.createElement("a");
    const file = new Blob([content], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = `${keyAlias}_${type}_${timestamp}.${format}`;
    document.body.appendChild(element); // Necessário para Firefox
    element.click();
    document.body.removeChild(element);
    addLog(`Download da chave ${type} iniciado.`);
  };

  return (
    <div className="keymanager-container">
      <header className="keymanager-header">
        <div className="brand-small">CRYPTO <span>QUANTUM</span> SERVICE</div>
        <Link to="/" className="back-link">
          <ArrowLeft size={16} /> Dashboard
        </Link>
      </header>

      <div className="workspace">
        <aside className="config-sidebar">
          <EntropyMeter />
          <div>
            <h3>Key Configuration</h3>
            <p style={{ fontSize: '0.8rem', color: '#666', marginBottom: '20px' }}>
              Define parameters for the quantum generator.
            </p>

            <div className="form-group">
              <label>Algorithm</label>
              <select disabled><option>RSA (Rivest–Shamir–Adleman)</option></select>
            </div>

            <div className="form-group">
              <label>Key Alias (Custom Name)</label>
              <input
                type="text"
                value={keyAlias}
                onChange={(e) => setKeyAlias(e.target.value)}
                placeholder="Ex: my-secure-key"
                className="alias-input"
              />
            </div>

            <div className="form-group">
              <label>Key Size (Bits)</label>
              <select
                value={keySize}
                onChange={(e) => setKeySize(e.target.value)}
              >
                <option value="2048">2048-bit (Standard)</option>
                <option value="4096">4096-bit (High Security)</option>
              </select>
            </div>

            <div className="form-group">
              <label>Export Format</label>
              <select
                value={format}
                onChange={(e) => setFormat(e.target.value)}
              >
                <option value="pem">PEM (.pem)</option>
                <option value="der">DER (.der) - Coming Soon</option>
              </select>
            </div>
          </div>

          <button
            className="btn-generate"
            onClick={generateKeys}
            disabled={isGenerating}
          >
            {isGenerating ? (
              <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
                <Cpu className="spin" size={20} /> Processing...
              </span>
            ) : "Initialize QPU & Generate"}
          </button>

          <Link to="/vault" className="btn-vault-access">
            Access Key Vault
          </Link>
        </aside>

        <main className="results-area">
          <div>
            <h4 style={{ color: '#888', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '10px' }}>
              <ShieldCheck size={18} color="var(--accent-cyan)" /> Quantum Secure Logs
            </h4>
            <div className="terminal-log" ref={terminalRef}>
              {logs.map((log, index) => (
                <div key={index} className="log-entry">{log}</div>
              ))}
            </div>
          </div>

          <div className={`keys-container ${keysVisible ? 'active' : ''}`}>

            <div className="key-box">
              <div className="key-header">
                <span style={{ color: 'var(--accent-cyan)' }}>PUBLIC KEY</span>
                <div style={{ fontSize: '0.8rem', color: '#666' }}>Visible to all</div>
              </div>
              <textarea
                readOnly
                className="key-content"
                value={publicKey}
              />
              <button className="btn-download" onClick={() => downloadKey('public', publicKey)}>
                <Download size={14} /> Export Public
              </button>
            </div>

            <div className="key-box" style={{ borderColor: '#552222' }}>
              <div className="key-header">
                <span style={{ color: '#ff5555', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  PRIVATE KEY
                  <button
                    className="btn-icon"
                    onClick={() => setShowPrivateKey(!showPrivateKey)}
                    title={showPrivateKey ? "Hide" : "Reveal"}
                  >
                    {showPrivateKey ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </span>
                <div style={{ fontSize: '0.8rem', color: '#ff5555' }}>⚠ Keep Secret</div>
              </div>
              <textarea
                readOnly
                className={`key-content private-key-area ${showPrivateKey ? 'visible' : ''}`}
                value={privateKey}
              />
              <button className="btn-download" onClick={() => downloadKey('private', privateKey)}>
                <Download size={14} /> Export Private
              </button>
            </div>

          </div>
        </main>
      </div>
      <Footer />
    </div >
  );
};

export default KeyManager;
