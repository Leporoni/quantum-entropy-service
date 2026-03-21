import React, { useState, useEffect, useRef } from 'react';
import { entropyService } from '../services/api';
import type { EntropyAuditReport, AuditMetrics } from '../services/api';
import './EntropyLab.css';

const EntropyLab: React.FC = () => {
  const [report, setReport] = useState<EntropyAuditReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runAudit = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await entropyService.auditEntropy(4096); // More data for better visuals
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
      <header className="lab-header">
        <h1>Entropy Laboratory</h1>
        <button 
          className="audit-button" 
          onClick={runAudit} 
          disabled={loading}
        >
          {loading ? 'Analyzing...' : 'Run New Audit'}
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      <div className="audit-grid">
        {report?.results.map((result, index) => (
          <AuditCard key={index} result={result} />
        ))}
      </div>

      <section className="audit-description">
        <h3>How to Read This Data</h3>
        <p>
          <strong>Shannon Entropy:</strong> Ideal value is 8.0 bits/byte. Anything above 7.95 is excellent.
        </p>
        <p>
          <strong>Chi-Square (χ²):</strong> Measures uniformity. For 256 buckets, a value between 180 and 340 is ideal. 
          Extreme values indicate patterns (low χ²) or systematic bias (high χ²).
        </p>
        <p>
          <strong>Monte Carlo π:</strong> Estimates π using byte pairs as coordinates. 
          The closer to 3.14159, the more spatial uniformity the source provides.
        </p>
        <p>
          <strong>Visual Proof:</strong> The bitmap above represents raw entropy. 
          Look for geometric patterns, lines, or textures in the PRNG sources. 
          True Quantum entropy should appear as perfect "snow" (white noise).
        </p>
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

        const imageData = ctx.createImageData(64, 64); // 4096 pixels
        for (let i = 0; i < bytes.length; i++) {
          const val = bytes[i];
          const idx = i * 4;
          imageData.data[idx] = val;     // R
          imageData.data[idx + 1] = val; // G
          imageData.data[idx + 2] = val; // B
          imageData.data[idx + 3] = 255; // A
        }
        ctx.putImageData(imageData, 0, 0);
      }
    }
  }, [result]);

  const isQuantum = result.source.toLowerCase().includes('quantum');

  return (
    <div className={`audit-card ${isQuantum ? 'quantum' : ''}`}>
      <h3>{result.source}</h3>
      
      <div className="bitmap-container">
        <canvas ref={canvasRef} width={64} height={64} />
      </div>

      <div className="metrics-list">
        <div className="metric-item">
          <span className="metric-label">Shannon Entropy</span>
          <span className={`metric-value ${result.shannonEntropy > 7.9 ? 'highlight' : ''}`}>
            {result.shannonEntropy.toFixed(4)} bits/byte
          </span>
        </div>
        <div className="metric-item">
          <span className="metric-label">Chi-Square (χ²)</span>
          <span className={`metric-value ${result.chiSquare >= 180 && result.chiSquare <= 340 ? 'highlight' : 'warning'}`}>
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
          <span className={`metric-value ${result.compressionRatio > 0.99 ? 'highlight' : 'danger'}`}>
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