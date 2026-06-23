import { useLocation } from 'react-router-dom';
import { LOT1, LOT2 } from '../../app/navigation';

/**
 * Page placeholder propre pour les fonctions non encore implémentées : titre de la
 * fonction + mention « à venir ». Aucun mock métier complet (hors périmètre).
 */
export default function PlaceholderPage() {
  const { pathname } = useLocation();
  const item = [...LOT1, ...LOT2].find((i) => i.route === pathname);
  const titre = item ? item.label : 'Fonctionnalité';

  return (
    <div className="placeholder">
      <h1>{titre}</h1>
      <p>Cette fonctionnalité fait partie du Poste de travail unifié.</p>
      <span className="soon-tag">À venir</span>
    </div>
  );
}
