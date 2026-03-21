import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Zap, Shield, Microscope } from 'lucide-react';
import { entropyService } from '../services/api';
import type { EntropyAuditReport, AuditMetrics } from '../services/api';
import './EntropyLab.css';

const INTENSITY_LEVELS = [
  { label: 'Quick Scan', size: 2048, icon: <Zap size={14} />, description: 'Fast results, basic confidence.' },
  { label: 'Standard Audit', size: 8192, icon: <Shield size={14} />, description: 'Balanced precision and speed.' },
  { label: 'Deep Analysis', size: 32768, icon: <Microscope size={14} />, description: 'Exposes PRNG periodic failures.' }
];

const EntropyLab: React.FC = () => {
  const [report, setReport] = useState<EntropyAuditReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [intensity, setIntensity] = useState(8192);

  const runAudit = async (customSize?: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await entropyService.auditEntropy(customSize || intensity);
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Failed to run entropy audit');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    runAudit();
  }, []);

  return (
    <div className="entropy-lab-container">
      <nav className="lab-nav">
        <Link to="/" className="back-link">
          <ArrowLeft size={18} /> Back to Dashboard
        </Link>
      </nav>

      <header className="lab-header">
        <div className="header-title">
          <h1>Entropy Laboratory</h1>
          <p className="subtitle">Verifying Quantum Stochastic Superiority</p>
        </div>
        
        <div className="lab-controls">
          <div className="intensity-selector">
            {INTENSITY_LEVELS.map((level) => (
              <button
                key={level.size}
                className={`intensity-btn ${intensity === level.size ? 'active' : ''}`}
                onClick={() => setIntensity(level.size)}
                title={level.description}
              >
                {level.icon} {level.label}
              </button>
            ))}
          </div>
          <button 
            className="audit-button" 
            onClick={() => runAudit()} 
            disabled={loading}
          >
            {loading ? 'Analyzing...' : 'Run New Audit'}
          </button>
        </div>
      </header>

      {error && <div className="error-message">{error}</div>}

      <div className="audit-grid">
        {report?.results.map((result, index) => (
          <AuditCard key={index} result={result} />
        ))}
      </div>

      <section className="audit-description">
        <div className="info-card">
          <h3>📊 Statistical Indicators</h3>
          <p>
            <strong>Shannon Entropy:</strong> Measures information density. Ideal is 8.0 bits/byte. 
            Quantum entropy remains constant even at high intensity, while PRNGs may show slight decay.
          </p>
          <p>
            <strong>Chi-Square (χ²):</strong> The "Uniformity Test". For 256 byte buckets, a value near 255.0 is perfect. 
            Low values suggest "too much" order, while high values indicate systematic bias in the generator.
          </p>
          <p>
            <strong>Monte Carlo π:</strong> Uses byte pairs as spatial coordinates. A result closer to 3.14159 
            proves the source lacks "clusters" or "voids" in its distribution.
          </p>
        </div>

        <div className="info-card">
          <h3>🔐 Security & Patterns</h3>
          <p>
            <strong>Compression Ratio:</strong> True entropy is incompressible. 
            A ratio &lt; 100% means the algorithm found a pattern (repetitive sequence) and could shrink the data. 
            <strong> Quantum data usually exceeds 100%</strong> because adding compression headers to random noise increases size.
          </p>
          <p>
            <strong>Repetitions:</strong> Counts consecutive identical bytes. 
            While random data can have repetitions (p=1/256), a high count in PRNGs often points to 
            mathematical "cycles" or limited internal state.
          </p>
        </div>
      </section>
    </div>
  );
};

const AuditCard: React.FC<{ result: AuditMetrics }> = ({ result }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (canvasRef.current && result.base64Sample) {
      const ctx = canvasRef.current.getContext('2d');
      if (ctx) {
        const binaryString = window.atob(result.base64Sample);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i);
        }

        // We use a fixed 64x64 or larger view for the bitmap
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

  return (
    <div className={`audit-card ${isQuantum ? 'quantum' : ''}`}>
      <div className="card-tag">{isQuantum ? 'Recommended' : 'Baseline'}</div>
      <h3>{result.source}</h3>
      
      <div className="bitmap-container">
        <canvas ref={canvasRef} width={64} height={64} />
      </div>

      <div className="metrics-list">
        <div className="metric-item">
          <span className="metric-label">Shannon Entropy</span>
          <span className={`metric-value ${result.shannonEntropy > 7.95 ? 'highlight' : ''}`}>
            {result.shannonEntropy.toFixed(4)} bits/byte
          </span>
        </div>
        <div className="metric-item">
          <span className="metric-label">Chi-Square (χ²)</span>
          <span className={`metric-value ${Math.abs(result.chiSquare - 255) < 20 ? 'highlight' : 'warning'}`}>
            {result.chiSquare.toFixed(2)}
          </span>
        </div>
        <div className="metric-item">
          <span className="metric-label">Monte Carlo π</span>
          <span className="metric-value">
            {result.piEstimate.toFixed(5)}
          </span>
        </div>
        <div className="metric-item">
          <span className="metric-label">Compression Ratio</span>
          <span className={`metric-value ${result.compressionRatio >= 1.0 ? 'highlight' : 'danger'}`}>
            {(result.compressionRatio * 100).toFixed(2)}%
          </span>
        </div>
        <div className="metric-item">
          <span className="metric-label">Repetitions</span>
          <span className="metric-value">
            {result.repetitions}
          </span>
        </div>
      </div>
    </div>
  );
};

export default EntropyLab;