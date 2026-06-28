# CLAUDE.md — Poste de travail unifié (GAM Assurances)

App web qui **orchestre des workflows AU-DESSUS** de l'ERP (PROASSUR, Sage, OneBase) sans
être source de vérité transactionnelle. Lire `README.md` et `docs/adr/` avant de coder.

## Principes non négociables
- PROASSUR / Sage / OneBase restent les **systèmes de référence**. Le poste **lit, orchestre,
  suit des workflows** ; il ne recalcule ni ne stocke la vérité transactionnelle (pas de shadow ERP).
- **Boucle fermée** : tout statut terminal est réécrit vers son système de référence (via le bus).
- **Architecture hexagonale** : domaine pur (zéro dépendance framework) au centre ; `port/in`
  (use cases) + `port/out` (interfaces) ; `adapter/` branche la technique. **Aucun appel en dur**
  vers un système de référence : tout passe par des **adapters MOCK remplaçables**.
- Domaine en **français** (langage ubiquitaire). Lisibilité > astuce. Pas de Lombok (records).
- **Tests obligatoires** (unitaires + 1 boucle d'intégration Testcontainers par fonction).
  ADR tenus à jour. **Commits conventionnels.**

## Stack
Back **Java 21 + Spring Boot** (Maven), front **TypeScript + React (Vite)**, bus **RabbitMQ**,
persistance **PostgreSQL** (état du poste UNIQUEMENT), infra dev via `docker-compose`.
Package backend : `dz.gam.poste`. Group : `dz.gam`.

## Structure
- `backend/src/main/java/dz/gam/poste/<contexte>/` — un contexte hexagonal par fonction
  (`cheque`, `tableaubord`, `cotation`, `dpd`) : `domain/{model,event,port/in,port/out,service}`,
  `adapter/{in/web,in/messaging,out/persistence,out/messaging,out/...mock}`, `config`.
- `frontend/src/` — `app/` (shell : AppShell, Topbar, Sidebar, navigation.ts), `theme/gam.css`
  (charte GAM, design system partagé), `features/<fonction>/`.
- `contracts/` (schémas d'événements + OpenAPI), `docs/adr/`, `docker-compose.yml`.
- Bus : exchange topic unique `gam.poste.events` ; chaque contexte déclare ses queues/bindings.

## Outillage — PAS sur le PATH système ; exporter d'abord
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export PATH="$JAVA_HOME/bin:/c/Users/mehdi/tools/apache-maven-3.9.9/bin:$PATH"
```
(Node : `export PATH="/c/Program Files/nodejs:$PATH"`.) Maven hors winget → installé dans `~/tools`.

## Commandes
```bash
cd backend  && mvn test            # tests unitaires (rapides, sans Docker)
cd backend  && mvn verify          # + boucles d'intégration Testcontainers (nécessite Docker)
cd frontend && npm install && npm run build   # typecheck + build

# Dev (3 process, à relancer par session) :
docker-compose up -d               # RabbitMQ + PostgreSQL
cd backend  && mvn spring-boot:run # API :8080  (Swagger: /swagger-ui.html)
cd frontend && npm run dev         # UI  :5173
```
Les `*IT` s'auto-ignorent sans Docker. ⚠️ curl sous Git Bash : éviter les accents dans le
JSON (corrompus → HTTP 400).

## Pattern pour une nouvelle fonction (les 5 restantes)
Maquette HTML fournie dans `~/Downloads/Maquette_*.html` (à reproduire FIDÈLEMENT, design GAM ;
réutiliser `theme/gam.css`). Pour chaque fonction : (1) machine à états + contrats de ports +
mapping champ→source ; (2) contexte backend hexagonal (domaine, ports, service, **adapters mock**
PROASSUR/Sage/BPM/GED/SSO, bus, config en `application.yml` jamais en dur) + tests (unitaires +
1 IT de boucle) ; (3) feature React fidèle ; (4) brancher le **compteur de nav réel**
(`tableaubord/.../CompteursAgenceAdapter`) ; (5) `mvn verify` puis commits conventionnels.
Fonctions livrées : Suivi des chèques, Accueil agence, Demande de cotation, Accords d'échéancier (DPD),
Versement bancaire (preuve de paiement, ADR 0006).
Restantes (placeholders) : Dépôt Situation Financière, Attestations, Envois bureau d'ordre,
Demande d'expertise, Suivi des échéanciers, Créances & contentieux.

## Déclaration de sinistre (front + DECSIN, PWA — ADR 0009)
Fonction **mobile-first** à **deux faces** demandant les mêmes pièces : **AGA** (feature
`frontend/src/features/sinistre/`, hérite du contexte d'agence : saisie OU envoi de lien, suivi,
**Valider → PROASSUR**) et **client** (PWA autonome `modules/declarations-sinistre/`, offline-first).
Le backend de déclaration **existe déjà** (DECSIN) : **pas de contexte Java** ici. Tout passe par la
couche partagée **`shared/decsin/`** (alias Vite `@decsin`) : port + **mock** (dev) / **HTTP réel**
(`VITE_DECSIN_MODE=real`). PROASSUR (prefill/rattachement) via ce même adaptateur. **Rien en dur**
(URL, clé d'API, paramètres → `.env`). Règle de pièces UNIQUE dans `shared/decsin/pieces.ts`
(`peutEnvoyer`) : recto+verso constat + 4 faces ; assurance adverse si tiers. **Flux d'envoi 2 temps**
(pièce→référence, puis déclaration) = base de la synchro offline (Dexie + Workbox côté client).
**Deux mondes d'auth à NE PAS mélanger** : AGA = SSO/contexte (interne) ; client = lien + double
facteur (téléphone + code), session locale persistée, déconnexion = purge du local. Le poste et la
PWA client sont **installables** (vite-plugin-pwa). Détails : `modules/declarations-sinistre/README.md`.
**Socle de capture PARTAGÉ** (`shared/sinistre-ui/`, alias `@sinistre-ui`) réutilisé par les 3 usages
(AGA poste, AGA mobile, client) : véhicule = **5 vues** (sélecteur + silhouettes SVG en overlay,
verbatim des maquettes ; toit FACULTATIF), capture caméra/galerie, écran de **contrôle** (complétude
calculée + vignettes + lightbox). Côté AGA : page détail `/declaration-sinistre/:idLocal` (Valider →
PROASSUR si complet / Renvoyer). **Validation différée** : capture hors-ligne, rattachement PROASSUR
auto à la reconnexion (`aRattacher`). OCR : seam prévu (non implémenté).

Le socle `@sinistre-ui` est **GÉNÉRIQUE** : piloté par un **Catalogue** (`shared/sinistre-ui/catalogue.ts`)
— groupes, pièces, silhouettes, règle de complétude. Chaque domaine fournit son catalogue
(`@decsin` → `catalogueDeclaration` ; `@souscription` → `catalogueSouscription`). Les `.tsx` de `shared/`
résolvent React via `paths`/`@types/react` dans les tsconfig.

## Souscription auto (front + GAM /SecGam, PWA agent — ADR 0010)
Fonction **mobile-first** calquée sur la déclaration, d'après l'APK « UNF Expert GAM ». Domaine
**`shared/souscription/`** (alias `@souscription`) : port + **mock**/**HTTP réel** (`VITE_SOUSCRIPTION_MODE`),
endpoints `/SecGam/*` (`api2.gam.dz/APIS/api/v2`), envoi **2 temps** (`postFile`→`enregistrerSouscription`).
**Produit AUTO** ; pièces obligatoires : CNI r/v + permis + carte grise + 4 faces véhicule (vues
complémentaires + permis verso facultatifs) ; `catalogueSouscription` (8 vues). Deux accès : **section
poste** `frontend/src/features/souscription/` (AGA, contexte d'agence — recherche police → stepper
produit/assuré → capture → contrôle → enregistrer) et **PWA agent autonome** `modules/souscription-auto/`
(offline Dexie, login agent + OTP, synchro 2 temps, installable). **OCR = seam non implémenté**
(`getOcrData`, mock figé). Auth = monde **agent** (login/mdp + OTP), distinct du monde client. **Rien en
dur** (`.env`). Détails : `modules/souscription-auto/README.md`.

## Reconnaissance véhicule + lecture de plaque (ANPR — ADR 0011)
Capacité **INDÉPENDANTE et REMPLAÇABLE** : vérifier qu'une photo est bien un véhicule et **lire la
plaque**, puis la comparer à l'**immatriculation du contrat** (PROASSUR) — aide qualité + garde-fou
anti-fraude. **Microservice autonome** `services/reco/` (Python/FastAPI, YOLO + fast-alpr, `POST
/analyser`, `GET /health`, `:8088`), Docker derrière le **profil compose `reco`** (opt-in). Le poste ne
parle qu'au **port** `dz.gam.poste.reconnaissance` (hexagonal) : `ReconnaissancePort` (mock par défaut
`reco.mode=mock` / réel `reco.mode=http` via `@ConditionalOnProperty`, clés à plat `reco.*` dans
`application.yml`, **rien en dur**). Une **panne du service ne bloque jamais** (résultat neutre).
Comparaison côté domaine `VerificationPlaqueService` → `CONFORME/NON_CONFORME/NON_LUE/PAS_UN_VEHICULE/
VUE_SANS_PLAQUE` (plaque attendue seulement sur `avant`/`arriere`). Endpoint `POST
/api/reconnaissance/analyser` (multipart `photo`+`vue`+`immatriculation`, session requise) ; anti-fraude
bloquante optionnelle (`reco.bloque-non-conforme`). Pas de boucle fermée (capacité synchrone) → tests
**unitaires purs** (pas d'IT Testcontainers). **Pas encore câblé au front** (affichage du statut dans
`DetailControlePage`/`DetailSouscriptionPage` + mapping vues `veh_*`→`avant…` = tranche suivante).
Détails : `services/reco/README.md`.

## Source des chiffres (substitut Cube Power BI, ADR 0007)
Les KPI de l'accueil et la situation mensuelle du versement proviennent du module
`dz.gam.poste.indicateursmock` : un **store persistant** (tables `mock_mesures_agence` /
`mock_situation_mensuelle`) derrière les 4 ports (`IndicateursProassurPort`,
`IndicateursSagePort`, `ProductionEncaissePort`, `VersementsBanquePort`). **Semé au démarrage**
avec des valeurs distinctes par feuille (depuis le périmètre du contexte) et **éditable** via
`/api/mock/indicateurs` (GET/PUT par agence et par agence+mois). À remplacer par un adapter
**Power BI** (mêmes ports) → suppression du store + de l'API mock, sans impact domaine.

## Identité / auth & contexte d'agence
**Authentification par login/mot de passe** (mock, ADR 0008) : page `/login` ; l'AGA se connecte
avec un **login + mot de passe** (gérés en admin), l'admin avec `admin`/`admin` au 1er démarrage
(modifiable + e-mail de récupération dans `/admin`). BCrypt (`spring-security-crypto`), session
serveur (HttpSession). Garde back : `AuthentificationInterceptor` → **401** hors `/api/auth/**`,
**403** sur `/api/admin/**` hors rôle ADMIN. Bouton **SSO Microsoft désactivé**
(`poste.auth.sso-microsoft-actif`) — Entra ID réel prévu plus tard. **Admin = espace d'admin
uniquement** (pas de modules métier).

**Profils & administration** : les profils SSO (AGA/agents + leurs agences + login/mdp) sont
**persistés** (table `contexte_profil`), semés depuis `poste.contexte.profils` au 1er démarrage,
et **gérables via une page Admin** (`/api/admin/profils` CRUD ; UI `/admin`). « Activer » un
profil = **aperçu admin / impersonation** (`POST /api/admin/identite/actif`, ADMIN seul). À
l'enregistrement d'un profil, un événement
`AgencesDeclareesEvent` est publié → les mocks sèment les chiffres (indicateurs) et chèques de
démo des nouvelles agences (découplage, pas de cycle entre modules).
**Accès par module** : chaque profil porte la liste des modules autorisés (`contexte_profil_module`).
Le menu est filtré côté front ET l'accès est appliqué côté back (`AccesModuleInterceptor` →
HTTP 403 sur `/api/{cheques|cotation|dpd|versement}` non autorisé ; l'accueil reste toujours
accessible). Config sans `modules` = accès complet (`Modules.TOUS`, résolu au seeding).

**Contexte d'agence (brique transverse `dz.gam.poste.contexte`, ADR 0005)** : l'identité fournit
un **périmètre d'agences** (mock `poste.contexte` : AGA M. Benzerga + 3 agences). L'**agence active**
est portée par la **session serveur** (HttpSession via `RequestContextHolder` ; repli vide hors
requête), jamais par le navigateur. Toutes les fonctions (accueil, versement, cotation, DPD,
chèques) lisent l'agence active via `AgenceCouranteQuery` et **ne redemandent jamais** l'agence :
`agencePourAction()` pour une action (création/dépôt/avancement), `agencesActives()` pour borner les
listes. SÉCURITÉ : périmètre **revérifié côté back** (hors périmètre → 403) ; ne jamais faire
confiance à l'agence envoyée par le front.
**Vue consolidée** (« Toutes mes agences », ≥2 agences) : vue d'ensemble **lecture seule** ;
toute action est **interdite** (`agencePourAction()` → HTTP 409). L'accueil agrège + répartition
par agence ; les listes montrent tout le périmètre.
