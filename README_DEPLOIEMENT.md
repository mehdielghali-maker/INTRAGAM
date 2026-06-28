# Déploiement & test sur téléphone — INTRAGAM

> **Contrainte clé** : sur mobile, la **caméra** et l'**installation de la PWA** exigent un
> **contexte sécurisé (HTTPS)**. Toute config de test ci-dessous aboutit à une URL `https://`.

Ce dépôt contient **4 cibles front PWA** distinctes (chacune son `package.json` + `vite-plugin-pwa`) :

| Cible | Dossier | Rôle | Back-end requis ? |
|---|---|---|---|
| Poste AGA | `frontend/` | Espace agence (login, contexte d'agence, toutes les fonctions) | **Oui** (Spring) → **Étage 2** |
| Client déclaration | `modules/declarations-sinistre/` | Le client déclare son sinistre (capture caméra) | **Non** (mock) → **Étage 1** |
| Agent souscription | `modules/souscription-auto/` | Agent terrain, souscription auto | **Non** (mock) → **Étage 1** |
| Client souscription | `modules/souscription-client/` | Le client finalise sa souscription (lien) | **Non** (mock) → **Étage 1** |

- **ÉTAGE 1** = les **3 PWA clientes/agent** en **mode mock**, hébergées en **statique (HTTPS)** →
  test immédiat sur téléphone (caméra + install), **sans back-end ni service RECO**.
- **ÉTAGE 2** = la **stack complète** (poste + back Spring + RECO) via `docker-compose`.

## Bascule mock ↔ réel
Aucun `if (mock)` dispersé : chaque domaine choisit son adaptateur dans sa fabrique
(`shared/*/config.ts` → `index.ts`). Variable **globale** :

- `VITE_API_MODE` = `mock` (défaut) | `real` — **repli** pour tous les domaines.
- Surcharge fine par domaine (prioritaire) : `VITE_DECSIN_MODE`, `VITE_SOUSCRIPTION_MODE`, `VITE_RECO_MODE`.
- `VITE_API_BASE_URL` / base-url par domaine : utilisées **uniquement en `real`**.

> En **Étage 1, laisser `VITE_API_MODE=mock`** (ou ne rien définir : c'est le défaut). **Aucun appel
> réseau**, tout le parcours tourne en données fictives. **Aucune clé secrète n'est nécessaire.**

---

# ÉTAGE 1 — Front mock, hébergement statique (HTTPS)

Chaque PWA se déploie comme **un projet statique séparé**. Fichiers fournis dans chaque dossier :
`vercel.json`, `netlify.toml`. Build : `npm run build` → sortie `dist/` (PWA : `manifest.webmanifest`
+ `sw.js` émis automatiquement). Fallback SPA configuré (les fichiers existants — `assets/`,
`manifest`, `sw.js` — sont servis **en priorité** ; les autres routes retombent sur `index.html`).
`sw.js` est servi en `Cache-Control: no-cache`.

## Option A — Vercel (recommandé)
Pour **chaque** PWA à déployer (répéter pour les 3) :
1. **New Project** → importer le dépôt GitHub `INTRAGAM`.
2. **Root Directory** = le sous-dossier de la PWA (ex. `modules/declarations-sinistre`).
3. ⚠️ **IMPORTANT (monorepo)** : activer **« Include files outside the Root Directory in the Build
   Step »** — sinon le dossier partagé `shared/` (résolu par les alias `@decsin`/`@sinistre-ui`/…)
   est introuvable au build.
4. **Framework** : Vite (auto-détecté). Build & sortie : déjà dans `vercel.json` (`npm run build`,
   `dist`). Rien à saisir.
5. **Environment Variables** : *(optionnel)* `VITE_API_MODE=mock` — ou rien (c'est le défaut).
6. **Deploy** → tu obtiens une URL `https://<projet>.vercel.app`.

Les 3 PWA → 3 projets Vercel (3 URLs https distinctes).

## Option B — Netlify
Pour chaque PWA : **Add new site → Import from Git** → **Base directory** = le sous-dossier
(`modules/…`). `netlify.toml` y fixe `command`, `publish=dist`, le fallback SPA (redirect `/* →
/index.html` 200) et le `no-cache` du `sw.js`. *(Variable optionnelle `VITE_API_MODE=mock`.)*

## Option C — Cloudflare Pages (via le tableau de bord)
**Create application → Pages → Connect to Git** → pour chaque PWA :
- **Root directory (advanced)** = le sous-dossier (`modules/…`).
- **Build command** = `npm run build`
- **Build output directory** = `dist`
- *(Variable optionnelle)* `VITE_API_MODE=mock`.

Cloudflare Pages sert les fichiers statiques en priorité ; pour le fallback SPA, ajouter un fichier
`public/_redirects` contenant `/*  /index.html  200` si une route profonde renvoie 404 (la plupart
des routes de ces PWA partent de `/`, donc rarement nécessaire).

## Tester sur le téléphone (PWA installée)
1. Ouvrir l'URL `https://…` de la PWA sur le téléphone.
2. **Installer** :
   - **Android (Chrome)** : menu ⋮ → **« Installer l'application »** (ou bannière d'ajout).
   - **iOS (Safari)** : **Partager** → **« Sur l'écran d'accueil »**.
3. **Autoriser la caméra** au premier accès (la capture des pièces/véhicule l'utilise). En HTTPS, le
   navigateur le permet ; en HTTP, la caméra est **bloquée**.
4. Parcours de test (mock) :
   - **Client déclaration** : ouvrir `…/?code=DEC-7F3A-2026` → OTP `0000` → capturer les faces.
   - **Client souscription** : ouvrir `…/?reference=SCR-TEST-2026` → OTP `0000` → capturer.
   - **Agent souscription** : login agent (OTP `0000`) → rechercher `AUTO-2026-00123` → capturer.

## Test « en direct » du serveur de dev (itération rapide, HTTPS par tunnel)
Sans héberger, on expose le serveur de dev via un **tunnel HTTPS** :
```bash
cd modules/declarations-sinistre      # ou souscription-auto / souscription-client
npm run dev:host                      # vite --host (écoute le réseau local)
# dans un autre terminal :
npm run dev:tunnel                    # cloudflared tunnel --url http://localhost:5174
```
`dev:tunnel` affiche une URL `https://<aléatoire>.trycloudflare.com` à ouvrir sur le téléphone.
**Prérequis** : installer `cloudflared` (`winget install Cloudflare.cloudflared`, `brew install
cloudflared`, ou binaire). Alternative : `ngrok http 5174`.

> **Option HTTPS local** (`localhost` uniquement) : ajouter `@vitejs/plugin-basic-ssl`
> (`npm i -D @vitejs/plugin-basic-ssl`, puis `basicSsl()` dans `plugins` du `vite.config.ts`).
> Utile sur la machine de dev, mais **pour tester DEPUIS le téléphone, le tunnel reste nécessaire**
> (le certificat auto-signé n'est pas reconnu par le mobile).

---

# ÉTAGE 2 — Stack complète (front poste + back Spring + RECO)

*(Voir la section dédiée plus bas — `docker-compose`, Dockerfiles, CORS, déploiement en ligne.)*

<!-- ÉTAGE 2 : complété dans le commit suivant (Dockerfiles + compose + README détaillé). -->
