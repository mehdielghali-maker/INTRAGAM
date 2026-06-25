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
          <h1>{tb.consolide ? `${tb.agence.nom} 👋` : `Bonjour, ${tb.agence.nom} 👋`}</h1>
          <div className="date">
            {tb.dateLibelle} · {contrats} contrats actifs ({variationContrats}) · arrêté en temps réel
          </div>
        </div>
      </div>

      {tb.consolide && (
        <div className="conso-banner">
          Vue consolidée — lecture seule. Pour effectuer une action (dépôt, demande…),
          choisissez une agence précise dans le commutateur en haut à droite.
        </div>
      )}

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

      {tb.consolide && tb.repartition.length > 0 && (
        <div className="panel" style={{ marginTop: 14 }}>
          <div className="panel-h">Répartition par agence</div>
          <div className="panel-sub">Contribution de chaque agence du périmètre</div>
          <div className="repartition-wrap">
            <table className="repartition">
              <thead>
                <tr>
                  <th>Agence</th>
                  <th>CA YTD</th>
                  <th>Encaissé</th>
                  <th>Versé</th>
                  <th>Écart</th>
                </tr>
              </thead>
              <tbody>
                {tb.repartition.map((r) => (
                  <tr key={r.code}>
                    <td>
                      <div className="ra-nom">{r.nom}</div>
                      <div className="ra-code">{r.code}</div>
                    </td>
                    <td>{montantDA(r.caYtd)}</td>
                    <td>{montantDA(r.encaisse)}</td>
                    <td>{montantDA(r.depose)}</td>
                    <td className="ra-ecart">{montantDA(r.ecart)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="footnote">
        Données de démonstration — chaque chiffre est lu en temps réel dans son système de
        référence (PROASSUR / Sage), sans recalcul ni stockage comme vérité dans le poste.
      </div>
    </>
  );
}
