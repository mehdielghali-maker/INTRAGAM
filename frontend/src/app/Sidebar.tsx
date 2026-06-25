import { NavLink } from 'react-router-dom';
import { Icon } from './icons';
import { ACCUEIL, ADMIN, LOT1, LOT2, NavItem } from './navigation';
import { CompteursNavigation } from '../features/accueil/types';

/** Sidebar de navigation : Accueil + 2 lots de fonctionnalités, avec badges compteurs. */
export default function Sidebar({ badges }: { badges: CompteursNavigation | null }) {
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
      <div className="nav-group">Lot 1 · Workflows &amp; demandes</div>
      {LOT1.map(renderItem)}
      <div className="nav-group">Lot 2 · Recouvrement</div>
      {LOT2.map(renderItem)}
      <div className="nav-group">Paramètres</div>
      {renderItem(ADMIN)}
    </nav>
  );
}
