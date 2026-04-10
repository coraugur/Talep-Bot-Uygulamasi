import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import TalepForm from './pages/TalepForm';
import TalepList from './pages/TalepList';
import TalepDetail from './pages/TalepDetail';
import PipelineDashboard from './pages/PipelineDashboard';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <nav className="navbar">
        <Link to="/" className="nav-brand">🤖 Talep Bot</Link>
        <div className="nav-links">
          <Link to="/">Talepler</Link>
          <Link to="/new">Yeni Talep</Link>
        </div>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<TalepList />} />
          <Route path="/new" element={<TalepForm />} />
          <Route path="/talep/:talepId" element={<TalepDetail />} />
          <Route path="/pipeline/:talepId" element={<PipelineDashboard />} />
        </Routes>
      </main>
    </BrowserRouter>
  );

}

export default App;
