import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CompteConnectable, connexion, getComptes, getConfig, getEtat, motDePasseOublie } from './api';
import './auth.css';

/** Logo Microsoft (4 carrés) pour le bouton SSO. */
function MicrosoftIcon() {
  return (
    <svg viewBox="0 0 21 21" width="18" height="18" aria-hidden="true">
      <rect x="1" y="1" width="9" height="9" fill="#f25022" />
      <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
      <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
      <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
    </svg>
  );
}

/**
 * Page de connexion (hors AppShell) : login + mot de passe pour l'admin ou un AGA. Bouton SSO
 * Microsoft présent mais désactivé tant que `ssoMicrosoftActif` est faux (activation ultérieure).
 * Lien « mot de passe oublié » affichant l'indice de l'adresse de récupération.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const [comptes, setComptes] = useState<CompteConnectable[]>([]);
  const [login, setLogin] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);
  const [ssoActif, setSsoActif] = useState(false);
  const [indice, setIndice] = useState<string | null>(null);

  // Déjà connecté ? on saute la page de login. Sinon on charge la liste des comptes + config SSO.
  useEffect(() => {
    getEtat()
      .then((p) => {
        if (p) navigate(p.role === 'ADMIN' ? '/admin' : '/', { replace: true });
      })
      .catch(() => undefined);
    getComptes()
      .then((c) => {
        setComptes(c);
        if (c.length > 0) setLogin(c[0].login); // 1re option (Administrateur) par défaut
      })
      .catch(() => setComptes([]));
    getConfig()
      .then((c) => setSsoActif(c.ssoMicrosoftActif))
      .catch(() => setSsoActif(false));
  }, [navigate]);

  async function soumettre(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setEnCours(true);
    try {
      const p = await connexion(login.trim(), motDePasse);
      navigate(p.role === 'ADMIN' ? '/admin' : '/', { replace: true });
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Connexion impossible');
    } finally {
      setEnCours(false);
    }
  }

  async function oublie() {
    setErreur(null);
    try {
      const r = await motDePasseOublie();
      setIndice(r.indiceEmail ? `${r.message} (${r.indiceEmail})` : r.message);
    } catch (err) {
      setErreur(err instanceof Error ? err.message : 'Récupération indisponible');
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={soumettre}>
        <div className="login-brand">
          <span className="login-logo">
            <img src="/logo-gam.webp" alt="GAM Assurances" />
          </span>
          <div className="login-titres">
            <h1>Poste de travail unifié</h1>
            <p>GAM Assurances — espace sécurisé</p>
          </div>
        </div>

        {erreur && <div className="login-msg ko">{erreur}</div>}
        {indice && <div className="login-msg info">{indice}</div>}

        <label className="login-field">
          Compte
          <select value={login} onChange={(e) => setLogin(e.target.value)}>
            {comptes.length === 0 && <option value="">Aucun compte disponible</option>}
            {comptes.map((c) => (
              <option key={c.login} value={c.login}>
                {c.type === 'ADMIN' ? c.libelle : `${c.libelle} (${c.login})`}
              </option>
            ))}
          </select>
        </label>
        <label className="login-field">
          Mot de passe
          <input
            type="password"
            value={motDePasse}
            autoComplete="current-password"
            placeholder="••••••••"
            onChange={(e) => setMotDePasse(e.target.value)}
          />
        </label>

        <button type="submit" className="login-submit" disabled={enCours || !login || !motDePasse}>
          {enCours ? 'Connexion…' : 'Se connecter'}
        </button>

        <button type="button" className="login-oublie" onClick={oublie}>
          Mot de passe oublié ?
        </button>

        <div className="login-sep">
          <span>ou</span>
        </div>

        <button
          type="button"
          className="login-sso"
          disabled={!ssoActif}
          title={ssoActif ? 'Connexion via Microsoft Entra ID' : 'Bientôt disponible'}
        >
          <MicrosoftIcon />
          Se connecter avec Microsoft
          {!ssoActif && <span className="login-sso-soon">Bientôt disponible</span>}
        </button>

        <p className="login-pied">Accès réservé. Données fictives (maquette).</p>
      </form>
    </div>
  );
}
