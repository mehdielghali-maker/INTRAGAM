import { Link } from 'react-router-dom';
import { Icon, IconName } from '../../../app/icons';
import { CompteurAction } from '../types';

/** Métadonnées d'affichage par clé d'action (libellés repris de la maquette). */
const META: Record<string, { icon: IconName; route: string; titre: string; sousTitre: string }> = {
  cheques: {
    icon: 'cheques',
    route: '/cheques',
    titre: 'Chèques en attente de statut',
    sousTitre: 'À pointer : imprimé · remis · encaissé',
  },
  attestations: {
    icon: 'attestations',
    route: '/attestations',
    titre: "Demandes d'attestation",
    sousTitre: 'À émettre depuis PROASSUR',
  },
  cotations: {
    icon: 'cotation',
    route: '/cotation',
    titre: 'Cotations en cours',
    sousTitre: 'Demande envoyée · devis attendu (central)',
  },
  echeanciers: {
    icon: 'echeanciers',
    route: '/echeanciers',
    titre: 'Échéanciers à risque',
    sousTitre: 'Échéance dépassée ou proche',
  },
  contentieux: {
    icon: 'contentieux',
    route: '/contentieux',
    titre: 'Créances en contentieux',
    sousTitre: 'Dossiers ouverts à suivre',
  },
};

export default function CoupDoeilPanel({ items }: { items: CompteurAction[] }) {
  return (
    <div className="panel">
      <div className="panel-h">Coup d'œil — à traiter</div>
      <div className="panel-sub">Ce qui attend votre action aujourd'hui</div>
      {items.map((item) => {
        const meta = META[item.cle];
        if (!meta) return null;
        return (
          <Link key={item.cle} to={meta.route} className="todo-line">
            <span className="todo-ic">
              <Icon name={meta.icon} />
            </span>
            <span className="todo-txt">
              <span className="t">{meta.titre}</span>
              <br />
              <span className="s">{meta.sousTitre}</span>
            </span>
            <span className={`todo-count ${item.urgent ? 'urg' : ''}`}>{item.valeur}</span>
            <Icon name="chevron" className="chev" />
          </Link>
        );
      })}
    </div>
  );
}
