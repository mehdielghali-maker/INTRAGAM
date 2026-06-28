# Déclaration de sinistre — PWA client + face AGA

Fonction « Déclaration de sinistre » du poste GAM, **mobile-first**, en **deux faces** qui
demandent les mêmes pièces (photos + documents) :

- **Face AGA** — dans le poste (`frontend/`, feature `src/features/sinistre/`). L'AGA saisit une
  déclaration ou **envoie un lien** au client, puis **suit, contrôle et valide → PROASSUR** (qui
  ouvre le dossier et renvoie le N° de sinistre). Hérite du contexte d'agence.
- **Face client** — cette PWA autonome (`modules/declarations-sinistre/`). Le client remplit la
  déclaration depuis son téléphone, **hors-ligne possible**, puis synchronisation automatique.

Le backend de déclaration **existe déjà** (DECSIN, `api2.gam.dz/DECSIN`) : on ne le reconstruit
pas. Tout passe par une **couche adaptateur unique** `shared/decsin/` (mutualisée par les 2 faces
via l'alias Vite `@decsin`), avec **bascule mock ↔ réel** par variable d'environnement.

## Socle de capture partagé `shared/sinistre-ui/`
Composants React mutualisés (alias `@sinistre-ui`) entre l'AGA (poste + mobile) et le client :
- **`CaptureVehicule`** : sélecteur des **5 vues** (avant/arrière/gauche/droite/**toit**) + **silhouette
  SVG** en overlay (fil de fer, verbatim des maquettes ; « droite » = miroir de « gauche »). Les
  **4 faces sont obligatoires, le toit est facultatif**. Calque de guidage — la photo n'est pas rognée.
- **`CaptureDocuments`** : Constat + Documents (caméra/galerie ; **seam OCR** documenté, non implémenté).
- **`PiecesCapture`** : orchestre les 3 groupes (layout `grille` poste / `liste` mobile-client).
- **`ApercusControle`** : bandeau de complétude (règle calculée) + vignettes + **lightbox** (détail AGA
  + récap client).

## Couche partagée `shared/decsin/`
- `types.ts` — modèles (Vehicule, Declaration, Piece, LoginResponse, statuts…).
- `pieces.ts` — 3 groupes (Constat / Véhicule / Documents) + **règle de validation unique**
  (`peutEnvoyer` / `piecesManquantes`) : recto+verso constat + 4 faces ; assurance adverse **si tiers**.
- `decsinPort.ts` — interface ; `decsinMock.ts` (dev) / `decsinHttp.ts` (réel) ; `index.ts` choisit.
- `media.ts` — compression image (canvas) avant stockage. `config.ts` — lecture `.env`.

## Lancer
```bash
# Couche partagée : aucune installation (TS pur, consommé via alias).
# Face AGA (poste) :
cd frontend && npm install && npm run dev      # http://localhost:5173 → « Déclaration de sinistre »
# Face client (PWA) :
cd modules/declarations-sinistre && npm install && npm run dev   # http://localhost:5174/?code=DEC-7F3A-2026
```
Le lien généré par l'AGA pointe vers la PWA client (`VITE_CLIENT_URL`, défaut `http://localhost:5174`).

## Configuration (`.env.example` → `.env.local`)
| Variable | Rôle | Défaut |
|---|---|---|
| `VITE_DECSIN_MODE` | `mock` (dev) ou `real` (backend) | `mock` |
| `VITE_DECSIN_BASE_URL` | base DECSIN | `https://api2.gam.dz/DECSIN/api/` |
| `VITE_DECSIN_API_KEY` / `_HEADER` | clé d'API + en-tête `[DSI à confirmer]` | — / `X-Api-Key` |
| `VITE_SESSION_TERRAIN_JOURS` | durée session client offline | `7` |
| `VITE_CODE_PREFIXE` | préfixe code (`DEC-XXXX-AAAA`) | `DEC` |
| `VITE_CLIENT_URL` (poste) | URL de la PWA client pour le lien | `http://localhost:5174` |

**Bascule réel** : `VITE_DECSIN_MODE=real` + URL/clé d'API → `decsinHttp` remplace `decsinMock`,
sans toucher au reste de l'app.

## Hors-ligne (PWA client)
- Service worker (Workbox) : app shell précaché, **ouverture hors-ligne** après 1ʳᵉ visite ;
  manifeste + **installable** (standalone).
- `navigator.storage.persist()` au démarrage ; **alerte** si refusé.
- **Dexie (IndexedDB)** : déclarations + pièces (blobs entiers) par déclaration et par type
  (dossier interne consultable hors-ligne).
- **File de synchro 2 temps** : chaque pièce → `PostAldFile` (référence), puis la déclaration →
  `SaveDeclarationAld`. Déclenchée au retour réseau et à l'ouverture. Idempotente (`idLocal`).
  Statut visible : en attente / synchronisé / échec.

## Authentification — deux mondes distincts
- **AGA** : SSO Microsoft/Entra ID (mock login/mdp actuel) + contexte d'agence (interne).
- **Client** : lien + **double facteur** (téléphone + code SMS ; mock = `0000`). Session locale
  persistée (offline) ; **déconnexion = purge du local**.

## Tests
```bash
cd modules/declarations-sinistre && npm test      # Vitest : règle de validation + synchro 2 temps
```

## À confirmer (DSI / métier)
Casse exacte des champs DECSIN et endpoints ; nom/valeur de la clé d'API ; mécanique précise du
double facteur ; durée de session terrain. En attendant, le mock couvre tout le parcours.
