import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import KeyManager from './pages/KeyManager';
import KeyVault from './pages/KeyVault';
import EntropyLab from './pages/EntropyLab';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/keymanager" element={<KeyManager />} />
        <Route path="/vault" element={<KeyVault />} />
        <Route path="/entropy-lab" element={<EntropyLab />} />
      </Routes>
    </Router>
  );
}

export default App;