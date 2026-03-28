import React from 'react';
import { Link } from 'react-router-dom';
import { Key, Microscope } from 'lucide-react';
import Footer from '../components/Footer';
import CloudStatus from '../components/CloudStatus';
import { cloudSyncService } from '../services/cloudSync';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  React.useEffect(() => {
    cloudSyncService.startSync();
  }, []);

  return (
    <div className="dashboard-container">
      <div className="bg-grid"></div>

      <header className="dashboard-header">
        <div className="brand">CRYPTO <span>QUANTUM</span> SERVICE</div>
        <CloudStatus />
      </header>

      <section className="hero">
        <h1>Pure Entropy. Absolute Security.</h1>
        <p>Harnessing quantum vacuum fluctuations to generate mathematically unpredictable cryptographic keys.</p>
      </section>

      <main className="services-container">
        <div className="service-card">
          <div className="card-icon">
            <Key size={48} />
          </div>
          <h2>Quantum Key Manager</h2>
          <p>
            Generation, management, and export of RSA key pairs (2048/4096 bits).
            Powered by our Quantum Entropy Engine (QRNG).
          </p>
          <div className="btn-group">
            <Link to="/keymanager" className="btn-primary">
              Access Generator
            </Link>
            <Link to="/vault" className="btn-secondary">
              <span className="btn-main-text">VAULT ACCESS</span>
              <span className="btn-sub-text">LIST | DELETE KEYS</span>
            </Link>
          </div>
        </div>

        <div className="service-card">
          <div className="card-icon">
            <Microscope size={48} />
          </div>
          <h2>Entropy Laboratory</h2>
          <p>
            Verify the mathematical superiority of Quantum entropy against standard
            Pseudo-Random Number Generators (PRNGs).
          </p>
          <Link to="/entropy-lab" className="btn-primary">
            Start Audit
          </Link>
        </div>
      </main>

      <Footer />
    </div>
  );
};

export default Dashboard;
