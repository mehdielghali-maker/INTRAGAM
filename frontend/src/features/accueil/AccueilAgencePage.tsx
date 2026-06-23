import { useOutletContext } from 'react-router-dom';
import { ContexteApp } from '../../app/AppShell';
import KpiCard from './components/KpiCard';
import KpiEcartCard from './components/KpiEcartCard';
import CoupDoeilPanel from './components/CoupDoeilPanel';
import ProductionDepotsPanel from './components/ProductionDepotsPanel';
import { montantDA, ratioPct } from './format';

/** Page d'accueil agence : tableau de bord de pilotage (5 KPI + coup d'œil + dépôts). */
export default function AccueilAgencePage() {
  const { tableauBord, erreur } = useOutletContext<ContexteApp>();

  if (erreur && !tableauBord) {
    return <div className="erreur-bloc">{erreur}</div>;
  }
  if (!tableauBord) {
    return <div className="loading">Chargement du tableau de bord…</div>;
  }

  const tb = tableauBord;
  const contrats = new Intl.NumberFormat('fr-FR').format(tb.contratsActifs);
  const variationContrats =
    (tb.contratsActifsVariation >= 0 ? '+' : '') + tb.contratsActifsVariation;

  return (
    <>
      <div className="hello">
        <div>
          <h1>Bonjour, {tb.agence.nom} 👋</h1>
          <div className="date">
            {tb.dateLibelle} · {contrats} contrats actifs ({variationContrats}) · arrêté en temps réel
          </div>
        </div>
      </div>

      <div className="kpis">
        <KpiCard
          icon="ca-ytd"
          label="CA Year‑to‑Date"
          valeur={montantDA(tb.caYtd.valeur)}
          variation={tb.caYtd.variation}
          sousLigne={`vs N‑1 : ${montantDA(tb.caYtd.reference)}`}
        />
        <KpiCard
          icon="ca-mois"
          label="CA du mois · à date"
          valeur={montantDA(tb.caMois.valeur)}
          variation={tb.caMois.variation}
          sousLigne={`vs M‑1 même jour : ${montantDA(tb.caMois.reference)}`}
        />
        <KpiEcartCard ecart={tb.ecartDepot} />
        <KpiCard
          icon="creances"
          label="Créances non recouvrées"
          valeur={montantDA(tb.creances.valeur)}
          variation={tb.creances.variation}
          sousLigne={`échues non encaissées · vs M‑1 : ${montantDA(tb.creances.reference)}`}
        />
        <KpiCard
          icon="sp"
          label="S/P · 12 mois glissants"
          valeur={ratioPct(tb.sp.valeur)}
          variation={tb.sp.variation}
          sousLigne={`émises · vs N‑1 : ${ratioPct(tb.sp.reference)}`}
        />
      </div>

      <div className="row2">
        <CoupDoeilPanel items={tb.coupDoeil} />
        <ProductionDepotsPanel data={tb.productionDepots} />
      </div>

      <div className="footnote">
        Données de démonstration — chaque chiffre est lu en temps réel dans son système de
        référence (PROASSUR / Sage), sans recalcul ni stockage comme vérité dans le poste.
      </div>
    </>
  );
}
