import { FormEvent, useState } from 'react';
import { Entite, souscription } from '@souscription';
import { useAgence } from '../../app/AgencyContext';
import SouscriptionStepper from './SouscriptionStepper';

/** Onglet « Nouvelle souscription » : recherche de la police puis parcours guidé. */
export default function NouvelleSouscriptionTab() {
  const { contexte } = useAgence();
  const consolide = contexte?.consolideActif ?? false;
  const agenceActive = contexte?.agenceActive ?? undefined;

  const [numeroPolice, setNumeroPolice] = useState('');
  const [nomClient, setNomClient] = useState('');
  const [resultats, setResultats] = useState<Entite[] | null>(null);
  const [choisie, setChoisie] = useState<Entite | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  async function rechercher(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setMessage(null);
    setChoisie(null);
    const r = await souscription.rechercheEntite({ numeroPolice: numeroPolice.trim(), nomClient: nomClient.trim() });
    setResultats(r);
    if (r.length === 0) {
      setErreur('Aucune police trouvée pour ces critères.');
    }
  }

  function terminer(msg: string) {
    setMessage(msg);
    setChoisie(null);
    setResultats(null);
    setNumeroPolice('');
    setNomClient('');
  }

  return (
    <div className="form-card">
      {erreur && <p className="bloc-msg" style={{ marginBottom: 12 }}>{erreur}</p>}
      {message && <p style={{ color: 'var(--vert)', marginBottom: 12, fontWeight: 600 }}>{message}</p>}

      <form className="loader" onSubmit={rechercher}>
        <div className="field">
          <label>N° de police</label>
          <input value={numeroPolice} placeholder="Ex. AUTO-2026-00123" onChange={(e) => setNumeroPolice(e.target.value)} />
        </div>
        <div className="field">
          <label>Nom du client</label>
          <input value={nomClient} placeholder="Ex. Meziane" onChange={(e) => setNomClient(e.target.value)} />
        </div>
        <button type="submit" className="btn-primary">Rechercher la police</button>
        <span className="hint-strong">Branche & véhicule se chargent depuis GAM</span>
      </form>

      {!choisie && resultats && resultats.length > 0 && (
        <table className="list" style={{ marginTop: 16 }}>
          <thead>
            <tr><th>N° police</th><th>Client</th><th>Branche</th><th>Véhicule</th><th>Action</th></tr>
          </thead>
          <tbody>
            {resultats.map((e) => (
              <tr key={e.numeroPolice}>
                <td><b>{e.numeroPolice}</b></td>
                <td>{e.nomClient}</td>
                <td>{e.libelleBranche}</td>
                <td>{e.marque ?? '—'} {e.immatriculation ? `· ${e.immatriculation}` : ''}</td>
                <td><button className="act-btn act-valider" onClick={() => setChoisie(e)}>Souscrire</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {choisie && (
        consolide ? (
          <p className="bloc-msg">Action impossible en vue consolidée : sélectionnez une agence.</p>
        ) : (
          <SouscriptionStepper
            entite={choisie}
            agence={agenceActive ? { code: agenceActive.code, nom: agenceActive.nom } : undefined}
            onTermine={terminer}
          />
        )
      )}
    </div>
  );
}
