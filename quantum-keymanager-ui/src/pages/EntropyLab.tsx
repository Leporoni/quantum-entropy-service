import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Zap, Shield, Microscope, Database, Terminal, Fingerprint } from 'lucide-react';
import { entropyService } from '../services/api';
import type { EntropyAuditReport, AuditMetrics, EntropyStatus } from '../services/api';
import Footer from '../components/Footer';
import './EntropyLab.css';

const INTENSITY_LEVELS = [
  { label: 'Quick Scan', size: 2048, icon: <Zap size={14} />, description: 'Fast results, basic confidence.' },
  { label: 'Standard Audit', size: 8192, icon: <Shield size={14} />, description: 'Balanced precision and speed.' },
  { label: 'Deep Analysis', size: 32768, icon: <Microscope size={14} />, description: 'Exposes PRNG periodic failures.' }
];

const EntropyLab: React.FC = () => {
  const [report, setReport] = useState<EntropyAuditReport | null>(null);
  const [status, setStatus] = useState<EntropyStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [intensity, setIntensity] = useState(8192);

  const fetchStatus = async () => {
    try {
      const data = await entropyService.getStatus();
      setStatus(data);
    } catch (err) {
      console.error('Failed to fetch entropy status');
    }
  };

  const runAudit = async (customSize?: number) => {
    setLoading(true);
    setError(null);
    try {
      const targetSize = customSize || intensity;
      const data = await entropyService.auditEntropy(targetSize);
      setReport(data);
      fetchStatus();
    } catch (err: any) {
      setError(err.message || 'Failed to run entropy audit');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
    runAudit();
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  const getAvailableBytes = () => status?.availableBytes || 0;

  return (
    <div className="entropy-lab-container">
      <nav className="lab-nav">
        <Link to="/" className="back-link">
          <ArrowLeft size={18} /> Back to Dashboard
        </Link>
        
        {status && (
          <div className="reservoir-status">
            <Database size={14} /> 
            <span>Reservoir: <strong>{(status.availableBytes / 1024).toFixed(1)} KB</strong> ({status.availableRecords} units)</span>
            <div className="reservoir-bar">
              <div 
                className="reservoir-fill" 
                style={{ width: `${Math.min(100, (status.availableBytes / 32768) * 100)}%` }}
              ></div>
            </div>
          </div>
        )}
      </nav>

      <header className="lab-header">
        <div className="header-title">
          <h1>Entropy Laboratory</h1>
          <p className="subtitle">High-Fidelity Quantum vs. PRNG Benchmarking</p>
        </div>
        
        <div className="lab-controls">
          <div className="intensity-selector">
            {INTENSITY_LEVELS.map((level) => {
              const isAvailable = getAvailableBytes() >= level.size;
              return (
                <button
                  key={level.size}
                  className={`intensity-btn ${intensity === level.size ? 'active' : ''} ${!isAvailable ? 'disabled' : ''}`}
                  onClick={() => isAvailable && setIntensity(level.size)}
                  title={isAvailable ? level.description : `Insufficient entropy (Need ${level.size / 1024} KB)`}
                >
                  {level.icon} {level.label}
                  {!isAvailable && <span className="lock-icon">🔒</span>}
                </button>
              );
            })}
          </div>
          <button 
            className="audit-button" 
            onClick={() => runAudit()} 
            disabled={loading || getAvailableBytes() < intensity}
          >
            {loading ? 'Analyzing...' : getAvailableBytes() < intensity ? 'Waiting for Entropy...' : 'Run New Audit'}
          </button>
        </div>
      </header>

      {error && <div className="error-message">{error}</div>}

      <div className="audit-grid">
        {report?.results && report.results.length > 0 ? (
          report.results.map((result, index) => (
            <AuditCard key={index} result={result} />
          ))
        ) : (
          !loading && <div className="no-data">Insufficient data. Wait for reservoir refill.</div>
        )}
      </div>

      <section className="audit-description">
        <div className="info-card">
          <h3>📊 Statistical Performance</h3>
          <p>
            <strong>Shannon Entropy:</strong> Aim for 8.0 bits/byte (100%). Even a 0.001 difference 
            at Deep Analysis scale represents millions of states of predictability in PRNGs.
          </p>
          <p>
            <strong>Chi-Square (χ²):</strong> The ultimate uniformity test. Offset from 255.0 measures 
            mathematical bias. Quantum sources typically show balanced, non-algorithmic offsets.
          </p>
          <p>
            <strong>Monte Carlo π:</strong> Approaching 3.14159 proving spatial equidistribution. 
          </p>
        </div>

        <div className="info-card">
          <h3>🛡️ Traceability & Audit</h3>
          <p>
            <strong>Traceability Fingerprint:</strong> The 16-byte HEX signature allows cross-referencing 
            this audit with raw logs from the <code>quantum-service</code>, proving the PURE nature of the source.
          </p>
          <p>
            <strong>Matrix Hex View:</strong> Real-time visualization of the first 64 bytes of the audit sample. 
            Notice the lack of rhythmic patterns or "software-generated" clusters in the Quantum source.
          </p>
        </div>
      </section>

      <Footer />
    </div>
  );
};

const AuditCard: React.FC<{ result: AuditMetrics }> = ({ result }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [hexPreview, setHexPreview] = useState<string[]>([]);

  useEffect(() => {
    if (result.base64Sample) {
      const binaryString = window.atob(result.base64Sample);
      const bytes = new Uint8Array(binaryString.length);
      const hexArr: string[] = [];
      
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
        if (i < 64) {
          hexArr.push(bytes[i].toString(16).padStart(2, '0').toUpperCase());
        }
      }
      setHexPreview(hexArr);

      // Canvas Bitmap
      const ctx = canvasRef.current?.getContext('2d');
      if (ctx) {
        const viewSize = Math.floor(Math.sqrt(bytes.length));
        const canvasSize = Math.min(viewSize, 128); 
        const imageData = ctx.createImageData(canvasSize, canvasSize);
        for (let i = 0; i < canvasSize * canvasSize; i++) {
          const val = bytes[i] || 0;
          const idx = i * 4;
          imageData.data[idx] = val;
          imageData.data[idx + 1] = val;
          imageData.data[idx + 2] = val;
          imageData.data[idx + 3] = 255;
        }
        ctx.putImageData(imageData, 0, 0);
      }
    }
  }, [result]);

  const isQuantum = result.source.toLowerCase().includes('quantum');
  const shannonPercent = (result.shannonEntropy / 8.0) * 100;
  const chiOffset = Math.abs(result.chiSquare - 255);

  return (
    <div className={`audit-card ${isQuantum ? 'quantum' : ''}`}>
      <div className="card-tag">{isQuantum ? 'Recommended' : 'Baseline'}</div>
      
      <div className="card-header-main">
        <h3>{result.source}</h3>
        <div className="trace-fingerprint" title="Traceability Fingerprint (First 16 Bytes)">
          <Fingerprint size={12} /> {result.fingerprintHex}
        </div>
      </div>
      
      <div className="visual-assets">
        <div className="asset-box">
          <span className="asset-label">Noise Bitmap</span>
          <div className="bitmap-container">
            <canvas ref={canvasRef} width={64} height={64} />
          </div>
        </div>
        
        <div className="asset-box">
          <span className="asset-label">Matrix Hex Preview</span>
          <div className="hex-matrix">
            {hexPreview.map((hex, i) => (
              <span key={i} className="hex-byte">{hex}</span>
            ))}
          </div>
        </div>
      </div>

      <div className="metrics-list">
        <div className="metric-item">
          <span className="metric-label">Shannon Entropy</span>
          <div className="metric-data">
            <span className={`metric-value ${result.shannonEntropy > 7.95 ? 'highlight' : ''}`}>
              {result.shannonEntropy.toFixed(5)} 
            </span>
            <span className="metric-sub">{shannonPercent.toFixed(3)}% of ideal</span>
          </div>
        </div>
        
        <div className="metric-item">
          <span className="metric-label">Chi-Square (χ²)</span>
          <div className="metric-data">
            <span className={`metric-value ${chiOffset < 30 ? 'highlight' : 'warning'}`}>
              {result.chiSquare.toFixed(2)}
            </span>
            <span className="metric-sub">Offset: {chiOffset > 0 ? '+' : ''}{chiOffset.toFixed(2)}</span>
          </div>
        </div>

        <div className="metric-item">
          <span className="metric-label">Monte Carlo π</span>
          <span className="metric-value">{result.piEstimate.toFixed(5)}</span>
        </div>
        
        <div className="metric-item">
          <span className="metric-label">Compression</span>
          <span className={`metric-value ${result.compressionRatio >= 1.0 ? 'highlight' : 'danger'}`}>
            {(result.compressionRatio * 100).toFixed(2)}%
          </span>
        </div>
      </div>
    </div>
  );
};

export default EntropyLab;