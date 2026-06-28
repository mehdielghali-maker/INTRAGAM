import { Catalogue, PieceCapturee } from './catalogue';
import CaptureDocuments from './CaptureDocuments';
import CaptureVehicule from './CaptureVehicule';
import './sinistre-ui.css';

/**
 * Socle de capture COMPLET, piloté par un CATALOGUE (déclaration, souscription…), partagé entre
 * toutes les faces. Le groupe « véhicule » (catalogue.groupeVehicule) est rendu via le sélecteur
 * de vues + silhouette ; les autres groupes en tuiles. Contrôlé par le parent.
 *
 * @param layout 'grille' (poste) ou 'liste' (mobile / client)
 * @param contexte contexte de validation passé au catalogue (ex. { tiers } pour la déclaration)
 */
export default function PiecesCapture({
  catalogue,
  contexte,
  pieces,
  layout = 'grille',
  onAjouter,
  onSupprimer,
}: {
  catalogue: Catalogue;
  contexte: unknown;
  pieces: PieceCapturee[];
  layout?: 'grille' | 'liste';
  onAjouter: (piece: PieceCapturee, blob: Blob) => void;
  onSupprimer: (type: string) => void;
}) {
  const estObligatoire = (type: string) => catalogue.estObligatoire(type, contexte);

  return (
    <div>
      {catalogue.groupes.map((g) =>
        g.id === catalogue.groupeVehicule ? (
          <div key={g.id}>
            <div className="sui-glabel">
              {g.libelle}
              {g.hint && <span className="cnt"> {g.hint}</span>}
            </div>
            <CaptureVehicule catalogue={catalogue} pieces={pieces} onAjouter={onAjouter} onSupprimer={onSupprimer} />
          </div>
        ) : (
          <div key={g.id}>
            <div className="sui-glabel">
              {g.libelle}
              {g.hint && <span className="cnt"> {g.hint}</span>}
            </div>
            <CaptureDocuments
              catalogue={catalogue}
              groupe={g.id}
              pieces={pieces}
              estObligatoire={estObligatoire}
              layout={layout}
              onAjouter={onAjouter}
              onSupprimer={onSupprimer}
            />
          </div>
        ),
      )}
    </div>
  );
}
