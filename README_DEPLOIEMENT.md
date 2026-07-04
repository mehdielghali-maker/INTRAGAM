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

## Option D — GitHub Pages (PERMANENT, gratuit, sans compte externe) ⭐
Un workflow **`.github/workflows/deploy-pages.yml`** construit les **3 PWA** (mode mock) et les publie
sur **GitHub Pages** à chaque push — URLs **permanentes en HTTPS**, sans Vercel/Netlify.

Activation (une seule fois) :
1. Le dépôt doit être **public** (Pages gratuit). *(Privé → nécessite GitHub Pro.)*
2. **Settings → Pages → Build and deployment → Source = « GitHub Actions »**.
3. Pousser (ou **Actions → Déploiement PWA → Run workflow**). Le déploiement se fait tout seul.

URLs obtenues (ex. dépôt `INTRAGAM` du compte `mehdielghali-maker`) :
- Accueil : `https://mehdielghali-maker.github.io/INTRAGAM/`
- `…/declarations-sinistre/?code=DEC-7F3A-2026` · `…/souscription-auto/` · `…/souscription-client/?reference=SCR-TEST-2026`
- OTP démo : **`0000`**. Caméra + installation PWA OK (HTTPS).

> Le **`base`** des PWA est paramétrable (`VITE_BASE`, défaut `/`) : le workflow le règle sur le
> sous-chemin Pages ; en local rien ne change. Le **poste** (login `benzerga`) n'est PAS publié ici
> (il lui faut le backend → Étage 2).

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

# ÉTAGE 2 — Stack complète (poste + back Spring + RECO)

Le **poste AGA** a besoin du back-end Java (login, contexte d'agence, fonctions métier) → il se lance
avec toute la stack. Fichiers fournis :
- `frontend/Dockerfile` (+ `frontend/nginx.conf`) : build Vite → **nginx** (fallback SPA, `sw.js`
  no-cache, gzip, **proxy `/api` → backend**). Le mode/URL d'API passent en **args de build** (`VITE_*`).
- `backend/Dockerfile` : multi-stage **Maven** → image **JRE slim**, profils Spring par env.
- `services/reco/Dockerfile` : **réutilisé tel quel** (non modifié).
- `docker-compose.full.yml` : `postgres` + `rabbitmq` + `reco` + `backend` + `front`, réseau interne,
  **healthchecks** (reco `GET /health` ; backend `GET /api/auth/config`), **cache des modèles** RECO
  (volume `reco-models`).
- `.env.example` (racine) : toutes les variables (front build, backend, CORS, infra). **Aucun secret commité.**

## Lancer en local
```bash
cp .env.example .env            # ajuster si besoin (la démo tourne en mock, sans clé)
docker compose -f docker-compose.full.yml up --build
```
- **Poste** : http://localhost:8081 (login démo `benzerga` / `gam2026`, semé au 1er démarrage).
- Le **front parle au backend** via `/api` (proxy nginx, **même origine → pas de CORS**) ; le **backend
  parle à RECO** (`RECO_MODE=http`, `RECO_BASE_URL=http://reco:8088`) ; **le front ne touche jamais reco.**
- Première construction : l'image **RECO télécharge les modèles** (YOLO/fast-alpr) → long + **RAM
  conséquente** ; ensuite mis en cache (volume `reco-models`).

## Modes mock/real dans la stack
- Le **poste** est bâti `VITE_API_MODE=real` (il appelle le backend Spring). La **déclaration** et la
  **souscription** restent `VITE_DECSIN_MODE=mock` / `VITE_SOUSCRIPTION_MODE=mock` par défaut (les backends
  externes DECSIN/SecGam ne sont **pas** requis pour la démo). Pour les passer en réel : renseigner
  `VITE_DECSIN_*` / `VITE_SOUSCRIPTION_*` (base-url + clés d'API) **côté front** — ce sont des appels
  **front → GAM**, pas via le backend Java.
- La **reconnaissance de plaque** du poste passe, elle, **par le backend** (`/api/reconnaissance`) → RECO.

## CORS
`CORS_ALLOWED_ORIGINS` (env du backend, **vide par défaut**) : à renseigner **uniquement** si le front est
servi sur une **autre origine** que le backend (ex. front sur Vercel + backend ailleurs). En compose,
le front et l'API sont en **même origine** (nginx) → CORS inutile. Valeurs sensibles (clé DECSIN, etc.)
**toujours par variables d'environnement de la plateforme**, jamais en dur.

## Déploiement en ligne (HTTPS)
- **PaaS conteneurs** (Railway / Render / Fly.io) : déployer chaque image (`reco`, `backend`, `front`)
  comme un service ; brancher les variables d'env (cf. `.env.example`) ; **le TLS/HTTPS est géré par la
  plateforme**. Régler `RECO_BASE_URL` sur l'URL interne du service reco, et — si le front est sur un
  domaine distinct du backend — `CORS_ALLOWED_ORIGINS` = l'origine du front + `VITE_API_BASE_URL` au build.
  ⚠️ **RECO consomme beaucoup de RAM** (modèles ML) → choisir une instance suffisamment dotée (≥ 2–4 Go).
- **VPS** : `docker compose -f docker-compose.full.yml up -d` derrière un reverse proxy **HTTPS
  automatique** — voir **`deploy/Caddyfile`** (Caddy, certificat Let's Encrypt auto ; un seul domaine
  suffit, le backend reste interne). Alternative : Traefik (labels) — même principe.

## Mise en production sur UN SERVEUR (pas à pas)

> Scénario recommandé : serveur interne GAM ou VPS Linux — l'OCR et la reconnaissance tournent
> alors **sur votre serveur**, aucune donnée d'assurance ne sort (contrainte de localité).
> Prérequis : Ubuntu/Debian, **8-16 Go de RAM**, ports 80/443 ouverts, un nom DNS (ex. `poste.gam.dz`).

```bash
# 1. Docker (si absent)
curl -fsSL https://get.docker.com | sh
# 2. Le projet
git clone https://github.com/mehdielghali-maker/INTRAGAM.git && cd INTRAGAM
# 3. Configuration — CHANGER les mots de passe
cp .env.example .env && nano .env      # POSTGRES_PASSWORD, RABBITMQ_PASSWORD → valeurs fortes
# 4. Construire et lancer (long la 1re fois : modèles ML, ensuite en cache)
docker compose -f docker-compose.full.yml up -d --build
# 5. HTTPS auto : mettre votre domaine dans deploy/Caddyfile puis ajouter le service caddy
#    au compose (le bloc à copier est EN COMMENTAIRE dans le Caddyfile). Relancer up -d.
# 6. Vérifier
curl -s http://localhost:8081/api/auth/config
```

Checklist production :
- [ ] changer **admin/admin** à la 1ʳᵉ connexion (`/admin`) et les mots de passe AGA de démo ;
- [ ] sauvegarde régulière du volume Postgres :
  `docker run --rm -v intragam_pgdata:/d alpine tar czf - /d > backup-$(date +%F).tgz` ;
- [ ] ⚠️ internet nécessaire **au premier démarrage** du conteneur reco (téléchargement unique des
  poids PaddleOCR — correctif « tout au build » en suivi) ; ensuite **zéro appel sortant** ;
- [ ] mises à jour : `git pull && docker compose -f docker-compose.full.yml up -d --build`.

## Récap des livrables Étage 2
`frontend/Dockerfile`, `frontend/nginx.conf`, `backend/Dockerfile`, `docker-compose.full.yml`,
`.env.example` (racine), `deploy/Caddyfile`, et le CORS configurable (`CORS_ALLOWED_ORIGINS`) côté backend.

