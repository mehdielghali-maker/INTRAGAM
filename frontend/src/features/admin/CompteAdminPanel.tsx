import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  changerMotDePasseAdmin,
  CompteAdmin,
  definirEmailRecuperation,
  getCompteAdmin,
} from './api';

/**
 * Panneau « Compte administrateur » : changer le mot de passe admin (ancien + nouveau +
 * confirmation) et définir l'adresse e-mail de récupération (mot de passe oublié).
 */
export default function CompteAdminPanel() {
  const [compte, setCompte] = useState<CompteAdmin | null>(null);
  const [ancien, setAncien] = useState('');
  const [nouveau, setNouveau] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [email, setEmail] = useState('');
  const [erreur, setErreur] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const charger = useCallback(async () => {
    try {
      const c = await getCompteAdmin();
      setCompte(c);
      setEmail(c.emailRecuperation ?? '');
    } catch (e) {
      setErreur(e instanceof Error ? e.message : 'Chargement du compte impossible');
    }
  }, []);

  useEffect(() => {
    charger();
  }, [charger]);

  async function changerMdp(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setMessage(null);
    if (nouveau !== confirmation) {
      setErreur('La confirmation ne correspond pas au nouveau mot de passe.');
      return;
    }
    if (!nouveau) {
      setErreur('Le nouveau mot de passe est requis.');
      return;
    }
    try {
      await changerMotDePasseAdmin(ancien, nouveau);
      setMessage('Mot de passe administrateur mis à jour.');
      setAncien('');
      setNouveau('');
      setConfirmation('');
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Changement impossible');
    }
  }

  async function enregistrerEmail(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setMessage(null);
    try {
      await definirEmailRecuperation(email.trim());
      setMessage('Adresse de récupération enregistrée.');
      await charger();
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Enregistrement impossible');
    }
  }

  return (
    <section className="adm-compte">
      <h2 className="page-h">Compte administrateur</h2>
      <p className="page-sub">
        Login : <b>{compte?.login ?? 'admin'}</b>. Modifiez le mot de passe et renseignez une
        adresse de récupération (utilisée en cas d'oubli).
      </p>

      {erreur && <p className="adm-msg ko">{erreur}</p>}
      {message && <p className="adm-msg ok">{message}</p>}

      <div className="adm-compte-grid">
        <form className="adm-form" onSubmit={changerMdp}>
          <h3>Changer le mot de passe</h3>
          <label className="adm-field">
            Mot de passe actuel
            <input
              type="password"
              autoComplete="current-password"
              value={ancien}
              onChange={(e) => setAncien(e.target.value)}
            />
          </label>
          <label className="adm-field">
            Nouveau mot de passe
            <input
              type="password"
              autoComplete="new-password"
              value={nouveau}
              onChange={(e) => setNouveau(e.target.value)}
            />
          </label>
          <label className="adm-field">
            Confirmer le nouveau mot de passe
            <input
              type="password"
              autoComplete="new-password"
              value={confirmation}
              onChange={(e) => setConfirmation(e.target.value)}
            />
          </label>
          <div className="adm-form-actions">
            <button type="submit" className="btn-primary" disabled={!ancien || !nouveau}>
              Mettre à jour le mot de passe
            </button>
          </div>
        </form>

        <form className="adm-form" onSubmit={enregistrerEmail}>
          <h3>Adresse de récupération</h3>
          <label className="adm-field">
            E-mail de récupération
            <input
              type="email"
              autoComplete="email"
              placeholder="ex. admin@gam.dz"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <p className="adm-hint">
            En cas de mot de passe oublié, un indice de cette adresse est affiché sur l'écran de
            connexion (envoi d'e-mail réel à venir).
          </p>
          <div className="adm-form-actions">
            <button type="submit" className="btn-primary">
              Enregistrer l'adresse
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
