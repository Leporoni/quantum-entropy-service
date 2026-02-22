import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import KeyManager from './pages/KeyManager';
import KeyVault from './pages/KeyVault';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/keymanager" element={<KeyManager />} />
        <Route path="/vault" element={<KeyVault />} />
      </Routes>
    </Router>
  );
}

export default App;