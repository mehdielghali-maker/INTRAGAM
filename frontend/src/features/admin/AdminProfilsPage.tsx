import { useCallback, useEffect, useState } from 'react';
import {
  activerProfil,
  AgenceProfil,
  creerProfil,
  getProfilActif,
  getProfils,
  modifierProfil,
  Profil,
  ProfilUtilisateur,
  supprimerProfil,
} from './api';
import './admin.css';

interface FormState {
  identifiant: string;
  nomAffiche: string;
  profil: ProfilUtilisateur;
  agences: AgenceProfil[];
  edition: boolean; // true = modification d'un profil existant
}

const FORM_VIDE: FormState = {
  identifiant: '',
  nomAffiche: '',
  profil: 'AGA',
  agences: [{ code: '', nom: '' }],
  edition: false,
};

export default function AdminProfilsPage() {
  const [profils, setProfils] = useState<Profil[]>([]);
  const [actif, setActif] = useState<string>('');
  const [form, setForm] = useState<FormState>(FORM_VIDE);
  const [erreur, setErreur] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const charger = useCallback(async () => {
    setErreur(null);
    try {
      setProfils(await getProfils());
      setActif(await getProfilActif());
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Chargement impossible');
    }
  }, []);

  useEffect(() => {
    charger();
  }, [charger]);

  function editer(p: Profil) {
    setMessage(null);
    setErreur(null);
    setForm({
      identifiant: p.identifiant,
      nomAffiche: p.nomAffiche,
      profil: p.profil,
      agences: p.agences.length ? p.agences.map((a) => ({ ...a })) : [{ code: '', nom: '' }],
      edition: true,
    });
  }

  function reinitialiser() {
    setForm(FORM_VIDE);
  }

  function majAgence(i: number, champ: keyof AgenceProfil, valeur: string) {
    setForm((f) => ({
      ...f,
      agences: f.agences.map((a, idx) => (idx === i ? { ...a, [champ]: valeur } : a)),
    }));
  }

  function ajouterAgence() {
    setForm((f) => ({ ...f, agences: [...f.agences, { code: '', nom: '' }] }));
  }

  function retirerAgence(i: number) {
    setForm((f) => ({ ...f, agences: f.agences.filter((_, idx) => idx !== i) }));
  }

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    setErreur(null);
    setMessage(null);
    const agences = form.agences
      .map((a) => ({ code: a.code.trim(), nom: a.nom.trim() }))
      .filter((a) => a.code && a.nom);
    if (!form.identifiant.trim() || !form.nomAffiche.trim() || agences.length === 0) {
      setErreur('Identifiant, nom affiché et au moins une agence (code + nom) sont requis.');
      return;
    }
    const payload: Profil = {
      identifiant: form.identifiant.trim(),
      nomAffiche: form.nomAffiche.trim(),
      profil: form.profil,
      agences,
    };
    try {
      if (form.edition) {
        await modifierProfil(payload);
        setMessage(`Profil « ${payload.nomAffiche} » mis à jour.`);
      } else {
        await creerProfil(payload);
        setMessage(`AGA « ${payload.nomAffiche} » créé avec ${agences.length} agence(s).`);
      }
      reinitialiser();
      await charger();
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Enregistrement impossible');
    }
  }

  async function activer(identifiant: string) {
    try {
      await activerProfil(identifiant);
      // Recharge l'app pour que le contexte (commutateur, écrans) reflète le profil activé.
      window.location.reload();
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Activation impossible');
    }
  }

  async function supprimer(identifiant: string) {
    if (!window.confirm(`Supprimer le profil « ${identifiant} » ?`)) return;
    try {
      await supprimerProfil(identifiant);
      await charger();
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Suppression impossible');
    }
  }

  return (
    <section>
      <h1 className="page-h">Administration — AGA & agences</h1>
      <p className="page-sub">
        Créez et gérez les AGA (et les agents) et leur périmètre d'agences. « Activer » simule
        la connexion de cet utilisateur ; ses agences et leurs chiffres de démo sont créés
        automatiquement.
      </p>

      {erreur && <p className="adm-msg ko">{erreur}</p>}
      {message && <p className="adm-msg ok">{message}</p>}

      <div className="adm-grid">
        {/* Liste des profils */}
        <div className="adm-liste">
          {profils.map((p) => (
            <div className={`adm-card ${p.identifiant === actif ? 'actif' : ''}`} key={p.identifiant}>
              <div className="adm-card-h">
                <div>
                  <div className="adm-nom">
                    {p.nomAffiche} <span className="adm-tag">{p.profil}</span>
                    {p.identifiant === actif && <span className="adm-tag on">actif</span>}
                  </div>
                  <div className="adm-id">{p.identifiant}</div>
                </div>
              </div>
              <ul className="adm-agences">
                {p.agences.map((a) => (
                  <li key={a.code}>
                    <b>{a.nom}</b> <span className="adm-code">{a.code}</span>
                  </li>
                ))}
              </ul>
              <div className="adm-actions">
                <button className="btn-ghost" onClick={() => activer(p.identifiant)} disabled={p.identifiant === actif}>
                  {p.identifiant === actif ? 'Connecté' : 'Activer'}
                </button>
                <button className="btn-ghost" onClick={() => editer(p)}>
                  Modifier
                </button>
                <button className="btn-ghost danger" onClick={() => supprimer(p.identifiant)}>
                  Supprimer
                </button>
              </div>
            </div>
          ))}
          {profils.length === 0 && <p className="adm-vide">Aucun profil.</p>}
        </div>

        {/* Formulaire */}
        <form className="adm-form" onSubmit={soumettre}>
          <h3>{form.edition ? `Modifier ${form.identifiant}` : 'Nouvel AGA / agent'}</h3>

          <label className="adm-field">
            Identifiant
            <input
              value={form.identifiant}
              disabled={form.edition}
              placeholder="ex. a.nouveau"
              onChange={(e) => setForm((f) => ({ ...f, identifiant: e.target.value }))}
            />
          </label>
          <label className="adm-field">
            Nom affiché
            <input
              value={form.nomAffiche}
              placeholder="ex. A. Nouveau"
              onChange={(e) => setForm((f) => ({ ...f, nomAffiche: e.target.value }))}
            />
          </label>
          <label className="adm-field">
            Type
            <select
              value={form.profil}
              onChange={(e) => setForm((f) => ({ ...f, profil: e.target.value as ProfilUtilisateur }))}
            >
              <option value="AGA">AGA (plusieurs agences)</option>
              <option value="AGENT">Agent (une agence)</option>
            </select>
          </label>

          <div className="adm-sub">Agences</div>
          {form.agences.map((a, i) => (
            <div className="adm-agence-row" key={i}>
              <input
                placeholder="Code (ex. 09.1.BLIDA)"
                value={a.code}
                onChange={(e) => majAgence(i, 'code', e.target.value)}
              />
              <input
                placeholder="Nom (ex. Agence Blida Centre)"
                value={a.nom}
                onChange={(e) => majAgence(i, 'nom', e.target.value)}
              />
              <button
                type="button"
                className="adm-x"
                onClick={() => retirerAgence(i)}
                disabled={form.agences.length <= 1}
                aria-label="Retirer l'agence"
              >
                ×
              </button>
            </div>
          ))}
          <button type="button" className="btn-ghost" onClick={ajouterAgence}>
            + Ajouter une agence
          </button>

          <div className="adm-form-actions">
            <button type="submit" className="btn-primary">
              {form.edition ? 'Enregistrer' : "Créer l'AGA"}
            </button>
            {form.edition && (
              <button type="button" className="btn-ghost" onClick={reinitialiser}>
                Annuler
              </button>
            )}
          </div>
        </form>
      </div>
    </section>
  );
}
