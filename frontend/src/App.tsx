import { Link, Outlet } from 'react-router-dom';

export default function App() {
  return (
    <div className="app">
      <header className="app-header">
        <Link to="/" className="app-title">
          Poste de travail unifié <span className="app-sub">— Suivi des chèques</span>
        </Link>
        <span className="app-org">GAM Assurances</span>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
