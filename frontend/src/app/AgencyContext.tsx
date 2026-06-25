import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { changerAgenceActive, ContexteAgence, getContexte } from './agence';

interface AgencyContextValue {
  /** Contexte courant (null tant que le chargement initial n'est pas terminé). */
  contexte: ContexteAgence | null;
  erreur: string | null;
  /** Change l'agence active (revérifiée côté back) puis rafraîchit le contexte. */
  changer: (code: string) => Promise<void>;
}

const AgencyContext = createContext<AgencyContextValue | null>(null);

/** Hook d'accès au contexte d'agence ; lève si utilisé hors du provider. */
export function useAgence(): AgencyContextValue {
  const ctx = useContext(AgencyContext);
  if (!ctx) {
    throw new Error('useAgence doit être utilisé dans un AgencyProvider');
  }
  return ctx;
}

/**
 * Fournit le contexte d'agence à toute l'application. Hydraté au démarrage par
 * GET /api/contexte (atterrissage direct sur l'accueil, sans écran de sélection).
 * L'agence active vit côté serveur (session) : ici on ne fait que la refléter.
 */
export function AgencyProvider({ children }: { children: ReactNode }) {
  const [contexte, setContexte] = useState<ContexteAgence | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    getContexte()
      .then(setContexte)
      .catch((e) => setErreur(e instanceof Error ? e.message : 'Contexte indisponible'));
  }, []);

  const changer = useCallback(async (code: string) => {
    const maj = await changerAgenceActive(code);
    setContexte(maj);
  }, []);

  return (
    <AgencyContext.Provider value={{ contexte, erreur, changer }}>{children}</AgencyContext.Provider>
  );
}
