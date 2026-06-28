import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import Topbar from './Topbar';
import Sidebar from './Sidebar';
import { AgencyProvider, useAgence } from './AgencyContext';
import { useAuth } from './AuthContext';
import { getNavigation, getTableauBord } from '../features/accueil/api';
import { CompteursNavigation, TableauBord } from '../features/accueil/types';

export interface ContexteApp {
  tableauBord: TableauBord | null;
  erreur: string | null;
}

/**
 * Coquille de l'application : barre supérieure (commutateur d'agence) + sidebar + zone de
 * contenu (Outlet). Le contexte d'agence est fourni par {@link AgencyProvider} ; le tableau
 * de bord et les badges sont rechargés à chaque changement d'agence active.
 */
export default function AppShell() {
  return (
    <AgencyProvider>
      <CoquilleApp />
    </AgencyProvider>
  );
}

function CoquilleApp() {
  const { contexte, changer } = useAgence();
  const principal = useAuth();
  const estAdmin = principal.role === 'ADMIN';
  const [tableauBord, setTableauBord] = useState<TableauBord | null>(null);
  const [badges, setBadges] = useState<CompteursNavigation | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  // Menu mobile : la sidebar est masquée en dessous de 1080px et s'ouvre en tiroir.
  const [menuOuvert, setMenuOuvert] = useState(false);

  // Clé de sélection : l'agence active, ou « CONSOLIDE » en vue d'ensemble.
  const selection = contexte
    ? contexte.consolideActif
      ? 'CONSOLIDE'
      : contexte.agenceActive?.code ?? null
    : null;

  // Recharge accueil + badges quand la sélection change (et au premier chargement,
  // une fois le contexte hydraté). Les données sont bornées à la sélection côté back.
  // L'admin n'a pas d'espace métier : on n'interroge pas le tableau de bord.
  useEffect(() => {
    if (estAdmin || !selection) return;
    setErreur(null);
    setTableauBord(null);
    getNavigation().then(setBadges).catch(() => setBadges(null));
    getTableauBord()
      .then(setTableauBord)
      .catch((e) => setErreur(e instanceof Error ? e.message : 'Erreur de chargement'));
  }, [estAdmin, selection]);

  return (
    <div>
      <Topbar
        contexte={contexte}
        principal={principal}
        onChanger={changer}
        onOuvrirMenu={() => setMenuOuvert(true)}
      />
      <div className="shell">
        <Sidebar badges={badges} ouvert={menuOuvert} onNaviguer={() => setMenuOuvert(false)} />
        {menuOuvert && (
          <div className="nav-backdrop" onClick={() => setMenuOuvert(false)} aria-hidden="true" />
        )}
        <main className="main">
          <Outlet context={{ tableauBord, erreur } satisfies ContexteApp} />
        </main>
      </div>
    </div>
  );
}
