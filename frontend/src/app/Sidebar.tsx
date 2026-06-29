import { NavLink } from 'react-router-dom';
import { Icon } from './icons';
import { useAgence } from './AgencyContext';
import { useAuth } from './AuthContext';
import { ACCUEIL, ADMIN, LOT1, LOT2, PRINCIPAUX, NavItem } from './navigation';
import { CompteursNavigation } from '../features/accueil/types';

/**
 * Sidebar de navigation selon le rôle : l'ADMIN ne voit que l'administration ; l'AGA voit
 * l'accueil + les lots filtrés selon les modules autorisés de son profil (pas d'administration).
 */
export default function Sidebar({
  badges,
  ouvert = false,
  onNaviguer,
}: {
  badges: CompteursNavigation | null;
  ouvert?: boolean;
  onNaviguer?: () => void;
}) {
  const { contexte } = useAgence();
  const principal = useAuth();
  const estAdmin = principal.role === 'ADMIN';
  const modules = contexte?.modules ?? [];
  // Accès accordé si le module est dans la liste autorisée du profil. Déclaration de sinistre
  // et souscription auto sont des fonctions front (GAM) toujours visibles pour l'AGA.
  const autorise = (item: NavItem) =>
    item.id === 'sinistre' || item.id === 'souscription' || modules.includes(item.id);

  const principaux = PRINCIPAUX.filter(autorise);
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

  function renderItem(item: NavItem, principal = false) {
    return (
      <NavLink
        key={item.id}
        to={item.route}
        end={item.route === '/'}
        onClick={onNaviguer}
        className={({ isActive }) =>
          `nav-item ${isActive ? 'active' : ''} ${item.soon ? 'soon' : ''} ${principal ? 'principal' : ''}`
        }
      >
        <Icon name={item.icon} className="ic" />
        <span className="lbl">{item.label}</span>
        {renderBadge(item)}
      </NavLink>
    );
  }

  if (estAdmin) {
    return (
      <nav className={`side ${ouvert ? 'ouvert' : ''}`}>
        <div className="nav-group">Administration</div>
        {renderItem(ADMIN)}
      </nav>
    );
  }

  return (
    <nav className={`side ${ouvert ? 'ouvert' : ''}`}>
      {renderItem(ACCUEIL)}
      {principaux.map((item) => renderItem(item, true))}
      {(lot1.length > 0 || lot2.length > 0) && <div className="nav-sep" />}
      {lot1.map((item) => renderItem(item))}
      {lot2.map((item) => renderItem(item))}
    </nav>
  );
}
