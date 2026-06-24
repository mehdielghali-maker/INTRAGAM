/**
 * Cellule « Souscripteur » (source BPM/OneBase). Affiche les initiales + nom dès qu'un
 * souscripteur est affecté (« En cours »), sinon l'état « Non affecté ».
 */
export default function SouscripteurCell({
  nom,
  initiales,
}: {
  nom: string | null;
  initiales: string | null;
}) {
  if (!nom) {
    return (
      <div className="sous empty">
        <span className="sav">—</span>
        <div>
          <div className="n">Non affecté</div>
          <div className="r">en attente central</div>
        </div>
      </div>
    );
  }
  return (
    <div className="sous">
      <span className="sav">{initiales}</span>
      <div>
        <div className="n">{nom}</div>
        <div className="r">Souscripteur central</div>
      </div>
    </div>
  );
}
