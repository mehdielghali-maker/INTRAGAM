import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import Topbar from './Topbar';
import Sidebar from './Sidebar';
import { getNavigation, getTableauBord } from '../features/accueil/api';
import { CompteursNavigation, Periode, TableauBord } from '../features/accueil/types';

export interface ContexteApp {
  tableauBord: TableauBord | null;
  erreur: string | null;
}

/**
 * Coquille de l'application : barre supérieure + sidebar + zone de contenu (Outlet).
 * Centralise l'état de période (piloté depuis la topbar) et le chargement du tableau
 * de bord, partagé avec la page d'accueil via le contexte d'Outlet.
 */
export default function AppShell() {
  const [periode, setPeriode] = useState<Periode>('YTD');
  const [tableauBord, setTableauBord] = useState<TableauBord | null>(null);
  const [badges, setBadges] = useState<CompteursNavigation | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    getNavigation().then(setBadges).catch(() => setBadges(null));
  }, []);

  useEffect(() => {
    setErreur(null);
    getTableauBord(periode)
      .then(setTableauBord)
      .catch((e) => setErreur(e instanceof Error ? e.message : 'Erreur de chargement'));
  }, [periode]);

  const moisAnnee = tableauBord ? tableauBord.periodeLibelle.split(' · ')[0] : '';

  return (
    <div>
      <Topbar
        agence={tableauBord?.agence ?? null}
        moisAnnee={moisAnnee}
        periode={periode}
        onPeriodeChange={setPeriode}
      />
      <div className="shell">
        <Sidebar badges={badges} />
        <main className="main">
          <Outlet context={{ tableauBord, erreur } satisfies ContexteApp} />
        </main>
      </div>
    </div>
  );
}
