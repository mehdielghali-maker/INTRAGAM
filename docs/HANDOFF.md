# Point de reprise (handoff)

> Document de passation pour reprendre le travail dans une nouvelle session.
> Voir aussi `CLAUDE.md` (règles projet) et `docs/adr/` (décisions).
> _Dernière mise à jour : 2026-06-28._

## Où on en est
- **Branche** : `feat/IntraGAM250626EGM` (PAS `main`). À jour avec `origin`.
  Dernier commit : `843cbe1` (workflow GitHub Pages). Le repo est **PRIVÉ**.
- **Remote** : `origin` = `https://github.com/mehdielghali-maker/INTRAGAM.git` (compte
  `mehdielghali-maker`, authentifié via GitHub CLI `gh` — scopes `repo, read:org, gist, workflow`).
  **Ex-PR INTRAGAM#1 = FERMÉE** (closed le 2026-06-25) → **aucune PR ouverte à ce jour**.
  *(Le repo client `mtirchi/INTRAGAM` était inaccessible → repo créé sous le compte connecté.)*
- **Tests** : back `mvn verify` vert (unitaires + boucles d'intégration Testcontainers) ;
  modules front (Vitest) verts (déclaration, souscription, socle `@dossier`).
- **Derniers ajouts** (tous commités + poussés) : auth login/mdp (ADR 0008), **déclaration de
  sinistre** (0009), **souscription auto** (0010), **reconnaissance véhicule + plaque** (0011),
  **workflow présentiel + brouillon** (0012), puis **déploiement étages 1 & 2** (voir section
  « Déploiement » plus bas). Détails dans les sections dédiées.
- **⚠️ État opérationnel en cours** (session du 2026-06-28) : choix d'hébergement **permanent =
  Vercel** (repo privé, gratuit) pour les 3 PWA ; le **poste** reste sur l'Étage 2 (backend). Le
  workflow GitHub Pages est poussé mais **inopérant tant que le repo est privé** (Pages gratuit =
  repo public). **RECO** en cours de validation en mode **réel** (Docker) — cf. section RECO.

## Connexion (nouveau — ADR 0008)
- **Page `/login`** : login + mot de passe (admin ou AGA) ; bouton « Se connecter avec Microsoft »
  **désactivé** (SSO Entra ID prévu plus tard, `poste.auth.sso-microsoft-actif: false`).
- **Compte admin** : login `admin`, **mot de passe initial `admin`** (semé au 1er démarrage,
  table `contexte_compte_admin`). Modifiable + e-mail de récupération dans `/admin` (panneau
  « Compte administrateur »). L'admin ne voit QUE l'administration.
- **Comptes AGA de démo** (logins/mots de passe semés depuis `application.yml`, hashés BCrypt) :
  `benzerga` / `gam2026` (AGA, 5 agences) · `saidi` / `gam2026` (agent, 1) ·
  `cherif` / `gam2026` (AGA, 2). Login + mot de passe gérés dans `/admin` (création/édition).
- **Aperçu admin** : « Activer » un profil dans `/admin` = impersonation (l'admin voit l'espace
  de l'AGA) ; pour revenir, se déconnecter et se reconnecter en `admin`.
- ⚠️ Si la base contient déjà d'anciens profils (sans login), ils ne peuvent pas se connecter :
  leur définir login + mot de passe dans `/admin`, ou recréer la base docker.

## Déclaration de sinistre (nouveau — ADR 0009)
Fonction mobile-first **2 faces**, sans backend Java (DECSIN externe via adaptateur front).
- **AGA** : feature `frontend/src/features/sinistre/` (nav « Déclaration de sinistre ») — saisie/lien,
  suivi, **Valider → PROASSUR**. Le poste est désormais **installable** (PWA, vite-plugin-pwa).
- **Client** : PWA autonome `modules/declarations-sinistre/` (offline-first, Dexie, synchro 2 temps).
  Lancer : `cd modules/declarations-sinistre && npm install && npm run dev` → `http://localhost:5174/?code=DEC-7F3A-2026`
  (OTP de démo : `0000`). Le lien généré par l'AGA pointe vers cette PWA (`VITE_CLIENT_URL`).
- **Couche partagée** : `shared/decsin/` (alias `@decsin`), bascule mock↔réel `VITE_DECSIN_MODE`.
  Tests : `cd modules/declarations-sinistre && npm test`. Détails : `modules/declarations-sinistre/README.md`.

## Souscription auto (nouveau — ADR 0010)
Calquée sur la déclaration, d'après l'APK « UNF Expert GAM ». Socle de capture `@sinistre-ui` rendu
**générique** (piloté par un **catalogue**) ; nouveau domaine **`shared/souscription/`** (`@souscription`,
port + mock/http `/SecGam/*`, bascule `VITE_SOUSCRIPTION_MODE`).
- **Section poste** : `frontend/src/features/souscription/` (nav « Souscription auto ») — recherche police →
  stepper (produit/assuré → capture → contrôle → enregistrer) + suivi + détail. Produit **AUTO**.
- **PWA agent autonome** : `cd modules/souscription-auto && npm install && npm run dev` → `http://localhost:5175`
  (login agent démo + **OTP `0000`**), offline-first. Pièces oblig. : CNI r/v + permis + carte grise + 4 faces.
- Tests : `cd modules/souscription-auto && npm test`. Détails : `modules/souscription-auto/README.md`.

## Reconnaissance véhicule + plaque (nouveau — ADR 0011)
Capacité **indépendante et remplaçable** : lire la plaque sur les photos véhicule et la comparer à
l'immatriculation du contrat (anti-fraude). **Microservice autonome** `services/reco/` (FastAPI/YOLO/
fast-alpr, `:8088`), opt-in via le profil compose `reco`. Côté back : contexte hexagonal
`dz.gam.poste.reconnaissance` (port + adapter mock/http par `@ConditionalOnProperty`, clés `reco.*`).
- **Endpoint** : `POST /api/reconnaissance/analyser` (multipart `photo`, `vue` facultatif,
  `immatriculation`, session requise) → `{statut, plaqueLue, typeVehicule, confiance, estVehicule, bloquant}`.
  Statut = `CONFORME|NON_CONFORME|NON_LUE|PAS_UN_VEHICULE|VUE_SANS_PLAQUE`.
- **Config** (`application.yml`) : `reco.mode` (mock défaut | http), `reco.base-url`, `reco.bloque-non-conforme`.
- **⚠️ MOCK ≠ vraie reconnaissance** (source de confusion fréquente — vérifié le 2026-06-28) : en
  `reco.mode=mock` (défaut dev), l'adapter renvoie **TOUJOURS** la plaque figée **`0987611616`** et
  **toujours** « voiture », quelle que soit la photo. Donc une vraie plaque ne « matche » jamais le
  contrat → **NON_CONFORME quasi systématique**, et un non-véhicule passe pour une voiture. **Ce n'est
  pas un bug** : toute la chaîne (microservice → adapter → domaine → front/PWA) est correcte et testée ;
  c'est juste que le mock est bidon. Pour une **vraie** lecture, lancer le microservice en mode http.
- **Lancer le VRAI service** (Python pas requis sur l'hôte, tout est dans Docker) :
  `docker compose --profile reco up -d --build reco-service` (1re fois : build torch + **téléchargement
  des modèles ML**, plusieurs min ; modèles ensuite cachés dans le volume `reco-models`). Health :
  `curl http://localhost:8088/health`. Puis backend en `http` : `mvn spring-boot:run` avec
  `-Dreco.mode=http -Dreco.base-url=http://localhost:8088` (le poste compare alors la VRAIE plaque lue).
  Pour la **PWA** `declarations-sinistre` : `VITE_RECO_MODE=real` + `VITE_RECO_BASE_URL=<url reco>`
  (appel navigateur direct → CORS `RECO_CORS_ORIGINS`). En Étage 2 (`docker-compose.full.yml`),
  `RECO_MODE=http` est déjà câblé → RECO réel en prod.
- **✅ Vrai service VALIDÉ en réel le 2026-06-28** (Docker) + **2 bugs réels corrigés** (le mode mock
  les masquait) : (1) `services/reco/Dockerfile` ne buildait pas (réseau PyPI + wheels CUDA inutiles) →
  **torch CPU-only + boucle de retries** (`d0d0ce5`) ; (2) `app.py` plantait en **HTTP 500 dès qu'une
  plaque était détectée** car `fast-alpr` renvoie `ocr.confidence` en **liste** et le code faisait
  `float(liste)` → helper `_coerce_conf` + `test_app.py` (`e545b86`). Preuves live : `bus.jpg` →
  `estVehicule:true/bus/0.873` ; `zidane.jpg` → `estVehicule:false` ; plaque synthétique → lue sans 500.
- **Panne RECO = jamais bloquante** (résultat neutre). Tests : unitaires purs (`VerificationPlaqueServiceTest`,
  `ReconnaissanceServiceTest`), pas d'IT (capacité synchrone, sans boucle fermée).
- **Front câblé** : composant partagé `@sinistre-ui/VerificationPlaque` affiche CONFORME/NON_CONFORME/NON_LUE
  sous le contrôle de complétude dans `DetailControlePage` (sinistre) + `DetailSouscriptionPage` (sélection
  face avant/arrière + mapping `face_*`/`veh_*`→vues RECO + appel `POST /api/reconnaissance/analyser`).
- **PWA `modules/declarations-sinistre/`** (offline, indépendante du Java) : RECO câblé à l'étape « Vérification »
  via la couche partagée **`@reco`** (`shared/reco/`, appel DIRECT au microservice, mock/http `VITE_RECO_MODE`,
  comparaison côté client `verifierPlaque`). Bandeau `VerificationPlaque` à analyseur injecté. CORS microservice
  (`RECO_CORS_ORIGINS`). Tests : `modules/declarations-sinistre` 20 verts (dont `@reco`).
- **Reste optionnel** : si `reco.bloque-non-conforme=true`, griser le bouton de validation côté front quand
  `bloquant` (aujourd'hui le statut est affiché mais le bouton reste actif). Détails : `services/reco/README.md`.

## Workflow présentiel + BROUILLON (nouveau — ADR 0012)
Correction du modèle (déclaration ET souscription) : **présentiel par défaut** (AGA en agence,
**BROUILLON** enregistrable même incomplet + reprise/édition + **auto-save débouncé**), **lien client =
exception**, **rattachement PROASSUR/GAM seulement à la validation** (brouillon = local-only).
- **Socle commun** `shared/dossier/` (alias `@dossier`) : machine à états unifiée
  `BROUILLON·LIEN_ENVOYE·A_VALIDER·RELANCE·VALIDEE` + auto-save (`creerAutoEnregistrement`) + interface
  `BrouillonStore`. Stores locaux : `shared/{decsin,souscription}/brouillonsLocaux.ts`.
- **Déclaration** : poste (`features/sinistre/` — stepper brouillon/auto-save/valider gated, suivi avec
  brouillons + « Reprendre », détail Reprendre/Valider/Renvoyer→RELANCE, reprise via `?reprendre`) +
  PWA client (`modules/declarations-sinistre/` — auto-save, statut LIEN_ENVOYE→A_VALIDER à l'envoi).
- **Souscription** : poste (`features/souscription/`, idem) + PWA agent (`modules/souscription-auto/`,
  auto-save) + **nouvelle PWA cliente** `modules/souscription-client/` (port 5176, lien + OTP, offline) ;
  action poste « Envoyer un lien au client ».
- Complétude (catalogue) → **valide** seulement, jamais le brouillon. **[DSI à confirmer]** : brouillon
  serveur (sinon local jusqu'à validation). Tests : socle 8, déclaration 31, souscription 6 (verts).
- **Nouvelle PWA à lancer** : `cd modules/souscription-client && npm i && npm run dev` (:5176 ?reference=…).

## Déploiement & test sur téléphone (session 2026-06-28)
Objectif : tester sur **téléphone** (caméra + PWA installable ⇒ **HTTPS obligatoire**) et déployer en
ligne, **sans toucher au métier** (seule une fabrique de bascule mock/real par env a été ajoutée).
- **Étage 1 — 3 PWA en mock, statique HTTPS** (commits `7c45405`, `2dcfc85`, `064d59b`) :
  `modules/{declarations-sinistre,souscription-auto,souscription-client}/` reçoivent `vercel.json` +
  `netlify.toml` (fallback SPA, `sw.js` no-cache), scripts `dev:host` + `dev:tunnel` (cloudflared) avec
  `allowedHosts` (tunnels autorisés dans les `vite.config`), `base` paramétrable (`VITE_BASE`, défaut
  `/`) + `start_url` relatif. Bascule globale **`VITE_API_MODE=mock`** (repli si `VITE_*_MODE` absent).
- **Étage 2 — stack complète** (commit `6a35266`) : `frontend/Dockerfile` (+ `nginx.conf` : SPA, proxy
  `/api`→backend), `backend/Dockerfile`, **`docker-compose.full.yml`** (postgres + rabbitmq + reco +
  backend + front, healthchecks, `RECO_MODE=http`), CORS configurable (`CORS_ALLOWED_ORIGINS` dans
  `WebAccesConfig`), `.env.example` (racine, **aucun secret**), `deploy/Caddyfile` (HTTPS auto VPS).
  Lancer : `docker compose -f docker-compose.full.yml up --build` → poste sur `:8081`.
- **Hébergement PERMANENT retenu = Vercel** (repo **privé** ⇒ Vercel gratuit gère le privé) : 1 projet
  par PWA, **Root Directory = `modules/<pwa>`** + cocher **« Include files outside of the Root
  Directory »** (indispensable : les PWA importent `shared/`). URLs `*.vercel.app` à la racine
  (`base=/`), redéploiement auto à chaque push. **Le poste** (login backend) ne va PAS en statique →
  Étage 2 sur Railway/Render/Fly (compte client). Détails : `README_DEPLOIEMENT.md`.
- **GitHub Pages** : workflow `.github/workflows/deploy-pages.yml` poussé (`843cbe1`, build des 3 PWA →
  Pages, `VITE_BASE=/<repo>/<app>/`). **Inopérant tant que le repo est privé** (Pages gratuit = repo
  public + Settings → Pages → Source = GitHub Actions). Conservé pour activation ultérieure.
- **Test téléphone immédiat (sans déployer)** : `cloudflared` est installé (winget). `cd modules/<pwa>
  && npm run dev:host` puis `npm run dev:tunnel` → URL `https://*.trycloudflare.com` (temporaire, vit le
  temps de la session). Le poste se tunnelise aussi (front `:5173` + backend `:8080` via proxy `/api`).

## Lancer l'app (3 process, à relancer chaque session)
Outillage hors PATH — exporter d'abord :
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export PATH="$JAVA_HOME/bin:/c/Users/mehdi/tools/apache-maven-3.9.9/bin:$PATH"
export PATH="/c/Program Files/nodejs:$PATH"          # Node
export PATH="/c/Program Files/GitHub CLI:$PATH"      # gh
docker-compose up -d                                 # RabbitMQ + PostgreSQL
cd backend  && mvn spring-boot:run                   # API :8080 (Swagger /swagger-ui.html)
cd frontend && npm run dev                           # UI  :5173
```
⚠️ curl sous Git Bash : **pas d'accents dans le JSON** (corrompus → HTTP 400).

## Historique — session « contexte d'agence + versement » (résumé des commits)
> _Travaux antérieurs. L'auth (ADR 0008), la déclaration (0009) et la souscription (0010)
> sont décrites dans leurs sections dédiées plus haut._
1. **Contexte d'agence** transverse (`dz.gam.poste.contexte`) + rebranch accueil. Agence active
   en session serveur ; périmètre revérifié back (403 hors périmètre) ; écart accueil aligné
   sur **encaissé − versé** (calcul unique `shared.regularisation.EcartRegularisation`).
2. **Versement bancaire** (`dz.gam.poste.versement`) : boucle fermée BPM (mock), GED OneBase,
   PROASSUR/Sage, capture photo + compression, suivi. Front `/versement`.
3. **Vue consolidée** « Toutes mes agences » (cumul + répartition) ; garde-fou : action
   interdite en consolidé (HTTP 409, `AgenceCouranteQuery.agencePourAction`).
4. **Alignement** cotation / DPD / chèques sur l'agence active (listes filtrées, actions
   bloquées en consolidé).
5. **Périmètre PLAT** d'agences (une agence = un point de vente, PAS de sous-agences — modèle
   corrigé après une 1re version hiérarchique abandonnée).
6. **Chiffres par agence éditables** (`indicateursmock`, substitut Cube Power BI) : tables
   `mock_mesures_agence` / `mock_situation_mensuelle` derrière les 4 ports ; API
   `/api/mock/indicateurs` ; semés distinctement par agence.
7. **Fiabilisation des IT** : conteneurs partagés (`IntegrationTestBase`) + résilience AMQP +
   `rerunFailingTestsCount=2` (le flake RabbitMQ de `BoucleDpdIT` est rejoué → build vert).
8. **Suivi des chèques** refait au design GAM (table.list + pastilles + cartes mobile) +
   **rafraîchissement auto** au changement d'agence (bouton « Rafraîchir » supprimé).
9. **Dashboard Admin** (front `/admin`) : créer/modifier/supprimer des **AGA et leurs agences**
   (persistés, table `contexte_profil`), « Activer » un profil (simule la connexion).
10. **Accès par module** par profil : cases à cocher dans l'admin ; menu filtré + **403 back**
    (`AccesModuleInterceptor`) sur les modules non autorisés.

ADR ajoutés : 0005 (contexte d'agence), 0006 (versement), 0007 (indicateurs/Power BI).

## API utiles pour piloter la démo (cookie de session = `curl -c/-b`)
- **Authentification** : `POST /api/auth/login {"login":"…","motDePasse":"…"}` (ouvre la session) ·
  `POST /api/auth/logout` · `GET /api/auth/etat` (401 si non connecté) ·
  `GET /api/auth/config` (`ssoMicrosoftActif`) · `POST /api/auth/mot-de-passe-oublie` (indice).
  ⚠️ TOUTES les autres API exigent une session (401 sinon) ; `/api/admin/**` exige le rôle ADMIN (403).
- **Compte admin** (ADMIN) : `GET /api/admin/compte` · `PUT /api/admin/compte/mot-de-passe
  {ancien,nouveau}` · `PUT /api/admin/compte/email-recuperation {email}`.
- **Aperçu profils** (ADMIN, impersonation) : `GET /api/admin/identite` (liste + actif) ·
  `POST /api/admin/identite/actif {"identifiant":"…"}`.
- **Admin profils** (ADMIN) : `GET/POST /api/admin/profils`, `PUT/DELETE /api/admin/profils/{id}`
  (corps : `{identifiant, login, motDePasse, nomAffiche, profil:"AGA|AGENT", agences:[{code,nom}], modules:[ids]}` ;
  `motDePasse` requis à la création, vide en édition = inchangé).
- **Contexte / agence active** : `GET /api/contexte` · `POST /api/contexte/agence-active {"code":"…"|"CONSOLIDE"}`.
- **Chiffres** : `GET/PUT /api/mock/indicateurs/{code}` et `…/{code}/situation/{mois}`.
- **Workflows mock** : `POST /api/mock/proassur/cheques` ; `POST /api/cotation/envoyer` +
  `POST /api/mock/cotation/{ref}/quittance` ; `POST /api/dpd/envoyer` + `POST /api/mock/dpd/{ref}/refuser` ;
  `POST /api/versement/soumettre`.
- Ids de modules (accès) : `depot, versement, attestations, cheques, bureau, cotation, expertise,
  accords, echeanciers, contentieux` (accueil toujours accessible).

## Points d'attention
- Profils de démo en config (`application.yml` › `poste.contexte.profils`) : `m.benzerga` (AGA,
  5 agences), `m.saidi` (agent, 1), `k.cherif` (AGA, 2). Un profil de test **`a.test`** a pu être
  créé en base pendant les tests (restreint au module `cheques`) — gérable/supprimable via `/admin`.
- Données mock en base (`mock_*`, `contexte_profil*`) — réinitialisables en recréant la base docker.
- `BoucleDpdIT` peut « flaker » (RabbitMQ Testcontainers) : c'est rejoué automatiquement, build vert.

## Prochaines pistes (au choix)
- Implémenter les **fonctions placeholder** restantes (6) : Dépôt Situation Financière, Attestations,
  Envois bureau d'ordre, Demande d'expertise, Suivi des échéanciers, Créances & contentieux
  (pattern hexagonal + maquette `~/Downloads/Maquette_*.html`, cf. `CLAUDE.md`).
- Brancher l'**adapter Power BI** réel (remplacer les `*StoreAdapter` d'`indicateursmock`, mêmes ports).
- **Auth Entra ID** réelle (le contexte est prêt à recevoir utilisateur + périmètre + modules depuis les claims).
- Gérer les **chiffres par agence dans le dashboard Admin** (aujourd'hui via `/api/mock/indicateurs`).
- Démarrer la **couche IA GAMIA** (non commencée à ce jour).
- Décider de la **stratégie de merge vers `main`** (aucune PR ouverte actuellement ; ouvrir une nouvelle
  PR depuis `feat/IntraGAM250626EGM` ou merger en direct selon le besoin client).
