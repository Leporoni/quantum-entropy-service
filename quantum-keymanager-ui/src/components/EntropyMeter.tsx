import React, { useEffect, useState } from 'react';
import { Fuel, RefreshCw } from 'lucide-react';
import { entropyService, type EntropyStatus } from '../services/api';
import './EntropyMeter.css';

const EntropyMeter: React.FC = () => {
  const [status, setStatus] = useState<EntropyStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchStatus = async () => {
    try {
      const data = await entropyService.getStatus();
      setStatus(data);
      setError(false);
    } catch (err) {
      console.error("Erro ao buscar entropia:", err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();

    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !status) return <div className="entropy-loader">Initializing sensors...</div>;

  const percentage = status ? Math.min((status.availableRecords / 1000) * 100, 100) : 0;

  let color = 'var(--accent-cyan)';
  let label = 'OPTIMAL';

  if (percentage < 20) {
    color = '#ff5555';
    label = 'CRITICAL';
  } else if (percentage < 50) {
    color = '#ffb86c';
    label = 'LOW';
  }

  return (
    <div className="entropy-meter-container">
      <div className="entropy-header">
        <div className="entropy-title">
          <Fuel size={14} color={color} />
          <span>QUANTUM FUEL</span>
        </div>
        <div className="entropy-label" style={{ color }}>{label}</div>
      </div>

      <div className="entropy-bar-bg">
        <div
          className="entropy-bar-fill"
          style={{
            width: `${percentage}%`,
            backgroundColor: color,
            boxShadow: `0 0 10px ${color}`
          }}
        ></div>
      </div>

      <div className="entropy-footer">
        <span>{status?.availableRecords || 0} units</span>
        <span className="entropy-update">
          {error ? "Connection failed" : <><RefreshCw size={10} className="spin" /> Synchronized</>}
        </span>
      </div>
    </div>
  );
};

export default EntropyMeter;
