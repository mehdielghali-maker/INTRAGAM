import { NavLink } from 'react-router-dom';
import { Icon } from './icons';
import { useAgence } from './AgencyContext';
import { ACCUEIL, ADMIN, LOT1, LOT2, NavItem } from './navigation';
import { CompteursNavigation } from '../features/accueil/types';

/** Sidebar de navigation : Accueil + lots filtrés selon les modules autorisés du profil. */
export default function Sidebar({ badges }: { badges: CompteursNavigation | null }) {
  const { contexte } = useAgence();
  const modules = contexte?.modules ?? [];
  // Accès accordé si le module est dans la liste autorisée du profil.
  const autorise = (item: NavItem) => modules.includes(item.id);

  const lot1 = LOT1.filter(autorise);
  const lot2 = LOT2.filter(autorise);

  function renderBadge(item: NavItem) {
    if (item.badgeTodo) {
      return <span className="nav-badge todo">{item.badgeTodo}</span>;
    }
    if (item.badgeKey) {
      const valeur = badges ? badges[item.badgeKey] : undefined;
      if (valeur === undefined) return null;
      return <span className={`nav-badge ${item.urgent ? 'urgent' : ''}`}>{valeur}</span>;
    }
    return null;
  }

  function renderItem(item: NavItem) {
    return (
      <NavLink
        key={item.id}
        to={item.route}
        end={item.route === '/'}
        className={({ isActive }) =>
          `nav-item ${isActive ? 'active' : ''} ${item.soon ? 'soon' : ''}`
        }
      >
        <Icon name={item.icon} className="ic" />
        <span className="lbl">{item.label}</span>
        {renderBadge(item)}
      </NavLink>
    );
  }

  return (
    <nav className="side">
      {renderItem(ACCUEIL)}
      {lot1.length > 0 && (
        <>
          <div className="nav-group">Lot 1 &middot; Workflows &amp; demandes</div>
          {lot1.map(renderItem)}
        </>
      )}
      {lot2.length > 0 && (
        <>
          <div className="nav-group">Lot 2 &middot; Recouvrement</div>
          {lot2.map(renderItem)}
        </>
      )}
      <div className="nav-group">Paramètres</div>
      {renderItem(ADMIN)}
    </nav>
  );
}
