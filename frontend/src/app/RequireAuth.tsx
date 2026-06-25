import { ReactNode, useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { getEtat, Principal } from '../features/auth/api';
import { AuthProvider } from './AuthContext';
import '../features/auth/auth.css';

type Etat = 'chargement' | 'connecte' | 'anonyme';

/**
 * Garde de l'espace protégé. Vérifie la session (GET /api/auth/etat) ; redirige vers /login si
 * non connecté. Applique la séparation des rôles : l'ADMIN ne voit que l'administration
 * (toute autre route → /admin) et l'AGA n'accède pas à l'administration (/admin → accueil).
 */
export default function RequireAuth({ children }: { children: ReactNode }) {
  const [etat, setEtat] = useState<Etat>('chargement');
  const [principal, setPrincipal] = useState<Principal | null>(null);
  const location = useLocation();

  useEffect(() => {
    getEtat()
      .then((p) => {
        setPrincipal(p);
        setEtat(p ? 'connecte' : 'anonyme');
      })
      .catch(() => setEtat('anonyme'));
  }, []);

  if (etat === 'chargement') {
    return <div className="auth-chargement">Chargement…</div>;
  }
  if (etat === 'anonyme' || !principal) {
    return <Navigate to="/login" replace />;
  }

  const surAdmin = location.pathname.startsWith('/admin');
  if (principal.role === 'ADMIN' && !surAdmin) {
    return <Navigate to="/admin" replace />;
  }
  if (principal.role === 'AGA' && surAdmin) {
    return <Navigate to="/" replace />;
  }

  return <AuthProvider principal={principal}>{children}</AuthProvider>;
}
