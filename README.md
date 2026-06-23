# Poste de travail unifié — GAM Assurances

Application web qui consolide sur un seul écran des données aujourd'hui éclatées
entre **PROASSUR** (core insurance), **OneBase** (BPM/GED) et **Sage** (comptabilité).

Le poste **lit, orchestre et suit des workflows** au-dessus de l'ERP. Il n'est
**jamais** source de vérité transactionnelle (pas de « shadow ERP »). Chaque statut
terminal d'un workflow est **réécrit** vers son système de référence (boucle fermée).

> Cette première tranche livre **« Suivi des chèques »** de bout en bout : un
> événement `ChequeEmis` (émis par l'adapter PROASSUR mock) crée un dossier de suivi ;
> l'utilisateur fait avancer le statut ; au statut terminal, le poste publie
> `ChequeStatutFinalise` et l'adapter PROASSUR mock journalise le write-back ERP.

## Architecture en une image

```
PROASSUR (mock)                  POSTE DE TRAVAIL UNIFIÉ
   │  ChequeEmis                    ┌───────────────────────────────┐
   ├──────────────────► RabbitMQ ──►│ adapter messaging (in)        │
   │                                │      │                        │
   │                                │   domaine (hexagone, pur)     │  ◄── React (Vite)
   │                                │      │  cycle de vie chèque   │      via REST
   │                                │      ▼                        │
   │  ChequeStatutFinalise          │ adapter messaging (out)       │
   │◄──────────────────  RabbitMQ ◄─┤                               │
   ▼  (write-back ERP, journalisé)  │ PostgreSQL : état du poste    │
                                    └───────────────────────────────┘
```

Voir [docs/adr/](docs/adr/) pour les décisions d'architecture.

## Pré-requis

- **Java 21** (`java -version`)
- **Node.js 20+** (`node --version`)
- **Docker + Docker Compose** (`docker compose version`)

## Lancer en local

### 1. Infrastructure (RabbitMQ + PostgreSQL)

```bash
docker-compose up -d
```

- RabbitMQ : http://localhost:15672 (guest / guest)
- PostgreSQL : localhost:5432 (poste / poste / poste)

### 2. Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run        # Windows : mvnw.cmd spring-boot:run
```

- API : http://localhost:8080/api/cheques
- OpenAPI / Swagger UI : http://localhost:8080/swagger-ui.html

### 3. Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

- UI : http://localhost:5173

## Démo de la boucle fermée

1. Simuler PROASSUR qui émet un chèque (adapter mock) :

   ```bash
   curl -X POST http://localhost:8080/api/mock/proassur/cheques \
     -H "Content-Type: application/json" \
     -d '{"reference":"CHQ-2026-0001","montant":150000.00,"beneficiaire":"SARL BATICONSTRUCT","agence":"Alger-Centre","dateEmission":"2026-06-23"}'
   ```

2. Le dossier apparaît dans l'UI (statut **Émis**). Faites-le avancer jusqu'à
   **Encaissé** (ou **Retourné**).
3. Au statut terminal, le poste publie `ChequeStatutFinalise`. Le log du backend
   affiche la ligne `WRITE-BACK PROASSUR` simulant la mise à jour du règlement.

## Accueil agence (tableau de bord)

La page d'accueil (`/`) est le tableau de bord de pilotage de l'agence : 5 KPI
(CA YTD vs N‑1, CA du mois vs M‑1, écart à régulariser, créances non recouvrées,
S/P 12 mois glissants), bloc « Coup d'œil — à traiter » et bloc « Production &
dépôts ». Une sidebar donne accès aux 9 fonctionnalités métier (2 lots) ; seules
« Accueil » et « Suivi des chèques » sont implémentées, les autres ouvrent une page
« à venir ».

- Données lues via adapters **mock** : `IndicateursProassurPort` (PROASSUR),
  `IndicateursSagePort` (Sage) ; le compteur « chèques en attente » est **réel**
  (agrégé des dossiers de suivi non terminaux).
- Le poste **dérive** la présentation (variations, écart, seuil) ; il ne recalcule
  aucune vérité comptable. Paramètres en config (`poste.tableau-bord.*`) :
  seuil de l'écart de dépôt, périmètre du numérateur S/P, période par défaut.
- API : `GET /api/agence/tableau-bord?periode=YTD|MOIS_COURANT`, `GET /api/agence/navigation`.

## Tests

```bash
cd backend
./mvnw test                   # tests unitaires (cycle de vie) — sans Docker
./mvnw verify                 # + test d'intégration boucle complète (nécessite Docker)
```

- **Unitaires** : règles de transition + cycle de vie du dossier (domaine pur).
- **Intégration** (`BoucleFermeeIT`) : `ChequeEmis` entrant → changement de statut →
  `ChequeStatutFinalise` → write-back. Utilise Testcontainers (RabbitMQ + PostgreSQL) ;
  automatiquement ignoré si Docker est absent.

## Structure du dépôt

| Dossier | Rôle |
|---|---|
| `contracts/` | Contrats d'intégration partagés : schémas d'événements + OpenAPI |
| `backend/` | Java 21 + Spring Boot. Domaine hexagonal + adapters (mock) |
| `frontend/` | TypeScript + React (Vite) |
| `docs/adr/` | Journal des décisions d'architecture |
| `docker-compose.yml` | Infrastructure de dev (RabbitMQ + PostgreSQL) |

## Périmètre

Cette session ne livre **que** le suivi des chèques. Les 8 autres fonctionnalités,
l'intégration aux **vraies** APIs éditeur, et la couche IA **GAMIA** sont hors
périmètre (emplacement réservé : `backend/src/main/java/dz/gam/poste/shared/gamia/`).
