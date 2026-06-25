import { createContext, ReactNode, useContext } from 'react';
import { Principal } from '../features/auth/api';

/** Utilisateur authentifié, fourni par {@link RequireAuth} à tout l'espace protégé. */
const AuthContext = createContext<Principal | null>(null);

export function AuthProvider({ principal, children }: { principal: Principal; children: ReactNode }) {
  return <AuthContext.Provider value={principal}>{children}</AuthContext.Provider>;
}

/** Hook d'accès à l'utilisateur connecté ; lève si utilisé hors de l'espace protégé. */
export function useAuth(): Principal {
  const principal = useContext(AuthContext);
  if (!principal) {
    throw new Error('useAuth doit être utilisé dans un espace protégé (RequireAuth)');
  }
  return principal;
}
