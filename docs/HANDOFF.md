# Point de reprise (handoff)

> Document de passation pour reprendre le travail dans une nouvelle session.
> Voir aussi `CLAUDE.md` (règles projet) et `docs/adr/` (décisions).
> _Dernière mise à jour : 2026-07-02._

## Où on en est
- **Branche** : `feat/IntraGAM250626EGM` (PAS `main`). À jour avec `origin` ; seul `docs/adr.rar`
  (fichier de l'utilisateur) reste non suivi. Dernier commit fonctionnel : `324de70` (les commits
  suivants sont des docs).
- **Repo PUBLIC** : `https://github.com/mehdielghali-maker/INTRAGAM.git` (passé public le 2026-06-29
  pour GitHub Pages). Compte `mehdielghali-maker`, `gh` authentifié (scopes `repo, read:org, gist,
  workflow`). Aucune PR ouverte. *(Repo client `mtirchi/INTRAGAM` inaccessible → repo du compte connecté.)*
- **⚠️ `main` est 54 commits EN RETARD** (au 2026-07-02) : tout le travail vit sur `feat/IntraGAM250626EGM`. Le
  harnais de l'agent **refuse de pousser sur `main`** (protection branche par défaut, même avec accord
  verbal) → le merge doit être fait PAR L'UTILISATEUR (PR GitHub ou `git push origin feat/IntraGAM250626EGM:main`).
- **Tests** : back `mvn verify` vert (unitaires — dont 11 des seeders de démo, unitaires purs — + IT
  Testcontainers) ; front verts (déclaration 32, qui **incluent** les 8 du socle `@dossier` ;
  souscription-auto 12 dont 6 démo) ; microservice : `services/reco/test_app.py`.
- **Au moment de la passation, RIEN ne tourne** (Docker arrêté, tunnels morts). Les 3 PWA restent
  servies en permanence par GitHub Pages (cf. Liens). Pour la démo du poste : section « Lancer l'app ».
- **Sessions 2026-06-28/29** (tout commité + poussé) : déploiement (Pages permanent + tunnels),
  **RECO validé en réel + 4 bugs corrigés + alertes à la capture + OCR chiffres/`*`**, menu réorganisé,
  **caméra réservée au mobile**, 3 bugs de capture mobile corrigés, **données de démo par agence**
  pour chaque module développé. Détails dans les sections dédiées.

## Liens permanents & démo
- **3 PWA (mock, HTTPS, PERMANENT — GitHub Pages, redéployé à chaque push)** :
  - Accueil : `https://mehdielghali-maker.github.io/INTRAGAM/`
  - Déclaration client : `…/INTRAGAM/declarations-sinistre/?code=DEC-7F3A-2026` (OTP `0000`)
  - Souscription agent : `…/INTRAGAM/souscription-auto/` (login agent + OTP `0000`)
  - Souscription client : `…/INTRAGAM/souscription-client/?reference=SCR-TEST-2026` (OTP `0000`)
  Workflow : `.github/workflows/deploy-pages.yml` (build 3 PWA, `VITE_BASE=/<repo>/<app>/`, mode mock).
  L'environnement `github-pages` autorise la branche `feat/IntraGAM250626EGM` (custom branch policy,
  réglé via API). Logs : `gh run list --workflow=deploy-pages.yml`.
- **Poste (backend requis)** : PAS d'URL permanente — démo via tunnel temporaire (cf. « Lancer l'app »,
  l'URL `*.trycloudflare.com` CHANGE à chaque relance). Pour du permanent : Étage 2 sur un PaaS.
- **Vercel = ABANDONNÉ** (404 / friction Root Directory + branche). Le projet `intragam` côté Vercel
  est mort ; **à supprimer par l'utilisateur** (sinon builds fantômes à chaque push). Le `vercel.json`
  racine (`439f25c`) et ceux des modules restent utilisables si on y revient, mais Pages fait le travail.

## Lancer l'app (à relancer chaque session — rien ne tourne au départ)
**Prérequis** : démarrer **Docker Desktop** (le daemon ne tourne pas seul sous Windows) ; Node v24
installé (`/c/Program Files/nodejs`) ; `npm install` nécessaire à la **1re** utilisation de chaque
front (`frontend/`, `modules/*`). Toutes les commandes se lancent depuis la **racine du repo**.
Outillage hors PATH — exporter d'abord :
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export PATH="$JAVA_HOME/bin:/c/Users/mehdi/tools/apache-maven-3.9.9/bin:$PATH"
export PATH="/c/Program Files/nodejs:$PATH"          # Node
export PATH="/c/Program Files/GitHub CLI:$PATH"      # gh
docker compose up -d                                 # RabbitMQ + PostgreSQL
cd backend  && mvn spring-boot:run                   # API :8080 (Swagger /swagger-ui.html) — RECO mock
cd frontend && npm run dev                           # UI  :5173
```
**Avec la VRAIE reconnaissance (recommandé pour la démo)** — EN PLUS du bloc ci-dessus (infra + front
inchangés), on ajoute le microservice et on remplace **seulement** la commande `mvn` :
```bash
docker compose --profile reco up -d --build reco-service   # --build IMPORTANT (bake app.py committé)
# attendre {"status":"ok","modeles_charges":true} sur http://localhost:8088/health (1er build : plusieurs min)
cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--reco.mode=http --reco.base-url=http://localhost:8088"
```
**Tunnel HTTPS pour téléphone** (cloudflared installé via winget) :
```bash
nohup "/c/Users/mehdi/AppData/Local/Microsoft/WinGet/Links/cloudflared.exe" tunnel --url http://localhost:5173 > /tmp/tp.log 2>&1 &
grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" /tmp/tp.log   # nouvelle URL à chaque lancement
```
⚠️ Lancer cloudflared **détaché (nohup)** sinon il meurt entre les tours de l'agent. Les `vite.config`
autorisent déjà `*.trycloudflare.com` (`allowedHosts`). Le proxy vite (`/api`→:8080) traverse le tunnel.
💡 **Démo RECO réel sur téléphone = passer par le POSTE tunnelé** (le backend appelle reco côté
serveur). Les PWA Pages sont mock-only ; la PWA déclaration en RECO réel exigerait un 2e tunnel
vers :8088 + `RECO_CORS_ORIGINS` — jamais nécessaire jusqu'ici.
**Ports** : 5173 poste · 5174 PWA déclaration · 5175 PWA agent · 5176 PWA cliente · 8080 API ·
8088 reco · 8081 stack complète (`docker-compose.full.yml`).
⚠️ curl sous Git Bash : **pas d'accents dans le JSON** (corrompus → HTTP 400).

## Connexion (ADR 0008)
- **Page `/login`** : login + mot de passe (admin ou AGA) ; bouton « Se connecter avec Microsoft »
  **désactivé** (SSO Entra ID prévu plus tard, `poste.auth.sso-microsoft-actif: false`).
- **Compte admin** : `admin` / **`admin`** au 1er démarrage (table `contexte_compte_admin`), modifiable
  + e-mail de récupération dans `/admin`. L'admin ne voit QUE l'administration.
- **Comptes AGA de démo** (semés depuis `application.yml`, BCrypt) : `benzerga` / `gam2026` (AGA,
  5 agences : `02.1.SAID`, `02.2.HYDRA`, `02.3.BMR`, `02.4.AISSAT`, `02.7.DRARIA`) ·
  `saidi` / `gam2026` (agent, 1) · `cherif` / `gam2026` (AGA, 2). Gérés dans `/admin`.
- **Aperçu admin** : « Activer » un profil dans `/admin` = impersonation ; se reconnecter en `admin` pour revenir.
- ⚠️ D'anciens profils en base sans login ne peuvent pas se connecter : leur définir login/mdp dans
  `/admin`, ou recréer la base docker.

## Déclaration de sinistre (ADR 0009)
Fonction mobile-first **2 faces**, sans backend Java (DECSIN externe via adaptateur front).
- **AGA** : `frontend/src/features/sinistre/` — saisie présentielle/lien, suivi, **Valider → PROASSUR**.
  Le poste est installable (PWA).
- **Client** : PWA `modules/declarations-sinistre/` (offline-first, Dexie, synchro 2 temps).
  Local : `npm run dev` → `http://localhost:5174/?code=DEC-7F3A-2026` (OTP `0000`). Permanent : Pages.
- **Couche partagée** : `shared/decsin/` (`@decsin`), bascule `VITE_DECSIN_MODE` (repli `VITE_API_MODE`).
  Tests : `cd modules/declarations-sinistre && npm test`. Détails : son `README.md`.

## Souscription auto (ADR 0010)
Calquée sur la déclaration (APK « UNF Expert GAM »). Domaine `shared/souscription/` (`@souscription`,
mock/http `/SecGam/*`, bascule `VITE_SOUSCRIPTION_MODE`). Socle de capture générique piloté par catalogue.
- **Poste** : `frontend/src/features/souscription/` — recherche police → stepper → capture → contrôle →
  enregistrer + suivi/détail. Produit **AUTO**.
- **PWA agent** : `modules/souscription-auto/` (:5175, login agent + OTP `0000`, offline).
- **PWA cliente** : `modules/souscription-client/` (:5176, lien + OTP, offline) — cf. ADR 0012.
- Pièces oblig. : CNI r/v + permis + carte grise + 4 faces. Tests : `npm test` (12, dont démo).

## Reconnaissance véhicule + plaque (ADR 0011) — VALIDÉE EN RÉEL
Capacité **indépendante et remplaçable** : microservice `services/reco/` (FastAPI/YOLO/fast-alpr, `:8088`,
profil compose `reco`) ; côté back contexte hexagonal `dz.gam.poste.reconnaissance` (mock défaut / http).
- **Endpoint poste** : `POST /api/reconnaissance/analyser` (multipart `photo`+`vue`+`immatriculation`,
  session requise) → `{statut, plaqueLue, typeVehicule, confiance, estVehicule, bloquant}`.
- **⚠️ MOCK ≠ vraie reconnaissance** : en `reco.mode=mock` (défaut dev), plaque figée `0987611616` +
  toujours « voiture » → NON_CONFORME quasi systématique et non-véhicules acceptés. **Pas un bug** ;
  pour une vraie lecture, lancer le microservice + `reco.mode=http` (cf. « Lancer l'app »).
- **✅ 4 bugs RÉELS corrigés en activant le mode http** (le mock les masquait tous) :
  1. `d0d0ce5` — l'image Docker ne buildait pas (wheels CUDA inutiles + réseau) → torch CPU + retries.
  2. `e545b86` — HTTP 500 dès qu'une plaque était lue (`ocr.confidence` en **liste** → `float()` KO)
     → `_coerce_conf` + `test_app.py`.
  3. `6757732` — multipart Spring sans filename → **422 « photo manquante »** → corps multipart
     construit à la main (boundary maîtrisé) dans `RecoHttpAdapter`.
  4. `6757732` — **RestClient négocie HTTP/2, uvicorn ne parle que HTTP/1.1** (« Invalid HTTP request
     received », cause principale) → client JDK forcé HTTP/1.1. + log WARN quand le repli neutre sert.
  Preuves live via le poste : bus→`bus`, personnes→`PAS_UN_VEHICULE`, plaque→lue.
- **Alertes DÈS LA CAPTURE** (`d021d7a`, `1123be5`) : `CaptureVehicule` lance l'analyse à chaque prise
  (analyseur injecté ; poste = backend, PWA = `@reco` direct) → bandeau par vue : ⚠ « pas un véhicule »
  (toutes vues), ✓ « plaque lue « … » », ℹ « plaque non lue » (avant/arrière). **Best-effort, jamais
  bloquant** (pièce ajoutée d'abord, panne/hors-ligne silencieuse). `VerificationPlaque` (page contrôle)
  **découplé de l'immatriculation** : PAS_UN_VEHICULE s'affiche même sans immat ; CONFORME/NON_CONFORME
  exigent toujours l'immat.
- **Plaques algériennes (11 chiffres) — limite structurelle** : TOUS les modèles fast-plate-ocr
  plafonnent à `max_plate_slots=10` (aucun ne lit 11). Atténuations en place (env compose committées) :
  `RECO_OCR_MODEL=cct-xs-v2-global-model` (préfixe fidèle ; le « s » invente des caractères),
  `RECO_PLAQUE_NUMERIQUE=true` (masque ONNX → chiffres only, fini « L » lu pour « 4 »),
  `RECO_PLAQUE_LONGUEUR=11` (complète les non-lus par `*`, ex. `5034471****`). ⚠️ Le `*` est ignoré
  par la normalisation mais **n'est PAS un joker** : une lecture tronquée (≤10 chiffres) reste
  NON_CONFORME face à un contrat à 11 chiffres — les `*` matérialisent le manque, c'est tout.
  Résultat utilisateur validé (chiffres lus justes + positions manquantes visibles). **Le vrai fix prod = modèle OCR ENTRAÎNÉ sur plaques DZ**
  (`max_plate_slots≥11`, CLI `fast_plate_ocr.cli.train`, branché par `RECO_OCR_MODEL` — ADR 0011,
  capacité remplaçable). Cf. `services/reco/README.md`.
- Panne RECO jamais bloquante (résultat neutre + WARN). PWA déclaration : appel direct `@reco`
  (`VITE_RECO_MODE`, CORS `RECO_CORS_ORIGINS`). Anti-fraude bloquante optionnelle
  (`reco.bloque-non-conforme`) : statut affiché, bouton non grisé côté front (reste optionnel).

## Workflow présentiel + BROUILLON (ADR 0012)
**Présentiel par défaut** (brouillon enregistrable même incomplet + reprise + auto-save débouncé),
**lien client = exception**, **rattachement PROASSUR/GAM à la validation seulement** (brouillon = LOCAL).
- Socle `shared/dossier/` (`@dossier`) : états `BROUILLON·LIEN_ENVOYE·A_VALIDER·RELANCE·VALIDEE`,
  helpers + `creerAutoEnregistrement`, interface `BrouillonStore`.
- Stores locaux : `shared/{decsin,souscription}/brouillonsLocaux.ts` (localStorage poste, Dexie PWA).
- Complétude (catalogue) → conditionne la **validation** seulement. **[DSI à confirmer]** : brouillon serveur.

## UI & capture mobile (session 2026-06-29)
- **Menu** (`935907c`) : **Souscription auto + Déclaration de sinistre en tête** (groupe `PRINCIPAUX`
  dans `navigation.ts`, style or `.nav-item.principal`), libellés « Lot 1/2 » **supprimés** (séparateur
  `.nav-sep`). Sidebar : `frontend/src/app/Sidebar.tsx`.
- **Caméra réservée au mobile** (`935907c`) : hook `shared/sinistre-ui/useEstMobile.ts`. Sur PC :
  pas de webcam — bouton **« Choisir une photo »** (véhicule) / **« Importer »** (documents). Sur
  téléphone : caméra in-app + silhouette. ⚠️ Détection rendue **robuste** (`324de70`) : `(pointer:
  coarse) OU (tactile + écran ≤1024px)` — le simple `hover:none` faisait passer un **Samsung S-Pen
  pour un PC** (bug réel utilisateur).
- **Crash mobile à la prise de photo** (`62b8118`) : caméra pleine résolution saturait la mémoire →
  l'onglet se RECHARGEAIT (perte de saisie). Fix : `getUserMedia` borné ~720p + canvas ≤1280px libéré
  + try/catch. Validé par l'utilisateur.
- **Silhouette invisible sur fond clair** (`cb08e00`) : trait blanc sans contraste en extérieur →
  `drop-shadow` sombre. Validé.
- **Vignette qui débordait en grille** (`066562e`) : `.sui-tile .ic img` non contraint → `object-fit:
  cover` + `overflow:hidden` + aperçu 72px pour les pièces capturées.

## Données de démo par agence (session 2026-06-29 — `e7501c8`)
Chaque module développé est peuplé PAR AGENCE (même mécanisme que les chèques : listeners de
`AgencesDeclareesEvent`, republié à chaque démarrage → seeds **idempotents** par référence déterministe) :
- **Cotation** : `proassurmock/CotationsDemoListener` — 3/agence (`DC-{agence}-001..003` :
  ENVOYEE, EN_COURS, A_FINALISER).
- **DPD/échéanciers** : `dpd/adapter/out/proassurmock/DpdDemoListener` — 3/agence (`DE-DEMO-…` :
  ENVOYEE, EN_VALIDATION, ACCORDEE + `AccordSuivi`).
- **Versement** : `versement/adapter/in/messaging/VersementsDemoListener` — 2/agence (DEPOSE →
  EN_CONTROLE → VALIDE via le BPM mock) ; idempotence via `existsByCodeAgence` (port + JPA + adapter).
- **Souscription (front mock global)** : `souscriptionsDemo()` dans `shared/souscription/souscriptionMock.ts`
  — 3 souscriptions (A_VALIDER, VALIDEE, LIEN_ENVOYE), semées quand le store est vide.
- Chèques + KPIs : déjà semés (inchangés). Déclaration : 4 démos front (inchangées).
- ⚠️ **Seeds FRONT = localStorage, semés seulement si la clé est ABSENTE**. Navigateur déjà utilisé →
  vider `souscription.mock` / `souscription.brouillons` / `decsin.mock.declarations` / `decsin.brouillons`
  (ou navigation privée). Les seeds BACK se vérifient via `GET /api/cotation|dpd|versement` (filtrés
  par l'agence active — basculer d'agence pour voir chaque lot).

## Déploiement (état final des sessions 2026-06-28/29)
- **Étage 1 — 3 PWA statiques** : `vercel.json`/`netlify.toml` par module, `dev:host`/`dev:tunnel`,
  `allowedHosts` tunnels, `base` paramétrable (`VITE_BASE`) + `start_url` relatif, bascule globale
  `VITE_API_MODE`. **Production effective = GitHub Pages** (cf. « Liens permanents »).
- **Étage 2 — stack complète** (`6a35266`) : `frontend/Dockerfile` (+nginx proxy `/api`),
  `backend/Dockerfile`, **`docker-compose.full.yml`** (postgres+rabbitmq+reco+backend+front,
  healthchecks, `RECO_MODE=http`), CORS par env (`CORS_ALLOWED_ORIGINS`), `.env.example` racine
  (aucun secret), `deploy/Caddyfile` (HTTPS auto VPS). Lancer :
  `docker compose -f docker-compose.full.yml up --build` → poste sur `:8081`. C'est LA voie pour un
  poste en ligne permanent (Railway/Render/Fly — compte client ; RAM RECO ≥2-4 Go).
- Détails : `README_DEPLOIEMENT.md`.

## Historique — session « contexte d'agence + versement » (résumé)
> _Travaux antérieurs, toujours exacts : contexte d'agence transverse (session serveur, 403 hors
> périmètre, vue consolidée lecture seule → 409), versement bancaire (boucle BPM), alignement
> cotation/DPD/chèques sur l'agence active, périmètre PLAT, chiffres éditables (`indicateursmock`,
> substitut Power BI, ADR 0007), fiabilisation IT (conteneurs partagés + rerun), suivi des chèques
> au design GAM, dashboard Admin (profils + agences persistés), accès par module (menu filtré + 403).
> ADR : 0005 (contexte), 0006 (versement), 0007 (indicateurs)._

## API utiles pour piloter la démo (cookie de session = `curl -c/-b`)
- **Auth** : `POST /api/auth/login {"login":"…","motDePasse":"…"}` · `POST /api/auth/logout` ·
  `GET /api/auth/etat` · `GET /api/auth/config`. ⚠️ Tout le reste exige une session (401) ;
  `/api/admin/**` exige ADMIN (403).
- **Admin** : `GET/PUT /api/admin/compte…` · `GET/POST /api/admin/identite…` (impersonation) ·
  CRUD `/api/admin/profils` (`motDePasse` requis à la création, vide en édition = inchangé).
- **Contexte** : `GET /api/contexte` · `POST /api/contexte/agence-active {"code":"…"|"CONSOLIDE"}`.
- **Chiffres** : `GET/PUT /api/mock/indicateurs/{code}` et `…/{code}/situation/{mois}`.
- **Workflows mock** : `POST /api/mock/proassur/cheques` ; cotation `envoyer` + `/api/mock/cotation/{ref}/quittance` ;
  dpd `envoyer` + `/api/mock/dpd/{ref}/refuser` ; `POST /api/versement/soumettre`.
- **RECO** : `POST /api/reconnaissance/analyser` (multipart `photo`+`vue`+`immatriculation`).
- Ids modules (accès) : `depot, versement, attestations, cheques, bureau, cotation, expertise,
  accords, echeanciers, contentieux` (accueil toujours accessible).

## Points d'attention
- **Push vers `main` refusé au harnais de l'agent** (branche par défaut) — merge à faire par l'utilisateur.
- Profils démo en config (`poste.contexte.profils`) ; un profil de test `a.test` peut exister en base
  (gérable via `/admin`). Données mock en base réinitialisables en recréant la base docker.
- `BoucleDpdIT` peut flaker (RabbitMQ Testcontainers) : rejoué automatiquement, build vert.
- `docs/adr.rar` : archive de l'utilisateur, non suivie — ne pas la committer/supprimer.
- Les URLs `*.trycloudflare.com` sont jetables ; toujours en donner une fraîche à l'utilisateur.
- Mock souscription front : `enregistrerSouscription` → VALIDEE ; `loginClient` OTP `0000`.

## Prochaines pistes (au choix)
- **Entraîner un modèle OCR plaques DZ** (`max_plate_slots≥11`) et le brancher via `RECO_OCR_MODEL`
  → lecture complète des 11 chiffres (seule vraie solution, cf. section RECO).
- **Mettre le poste en ligne** (Étage 2 sur Railway/Render/Fly, compte client) → URL permanente
  avec login + RECO réel, à ajouter à la page d'accueil Pages.
- **Merger vers `main`** (par l'utilisateur) et **supprimer le projet Vercel** `intragam`.
- Implémenter les **fonctions placeholder** restantes (6) : Dépôt Situation Financière, Attestations,
  Envois bureau d'ordre, Demande d'expertise, Suivi des échéanciers, Créances & contentieux.
- Brancher l'**adapter Power BI** réel (mêmes ports qu'`indicateursmock`).
- **Auth Entra ID** réelle ; chiffres par agence gérés dans le dashboard Admin ; couche IA **GAMIA**.
