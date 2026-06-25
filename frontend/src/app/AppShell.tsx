import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import Topbar from './Topbar';
import Sidebar from './Sidebar';
import { AgencyProvider, useAgence } from './AgencyContext';
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
  const [tableauBord, setTableauBord] = useState<TableauBord | null>(null);
  const [badges, setBadges] = useState<CompteursNavigation | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);

  // Clé de sélection (sous-agence, groupe parent ou consolidé) : recharge à chaque changement.
  const selection = contexte?.selectionCode ?? null;

  // Recharge accueil + badges quand la sélection change (et au premier chargement,
  // une fois le contexte hydraté). Les données sont bornées à la sélection côté back.
  useEffect(() => {
    if (!selection) return;
    setErreur(null);
    setTableauBord(null);
    getNavigation().then(setBadges).catch(() => setBadges(null));
    getTableauBord()
      .then(setTableauBord)
      .catch((e) => setErreur(e instanceof Error ? e.message : 'Erreur de chargement'));
  }, [selection]);

  return (
    <div>
      <Topbar contexte={contexte} onChanger={changer} />
      <div className="shell">
        <Sidebar badges={badges} />
        <main className="main">
          <Outlet context={{ tableauBord, erreur } satisfies ContexteApp} />
        </main>
      </div>
    </div>
  );
}
