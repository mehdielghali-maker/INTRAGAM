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

## Identité / auth & contexte d'agence
Pas d'auth réelle : identité **SSO (Entra ID) mockée** en config. Le bloc legacy « Code Accès »
est supprimé au profit de l'identité SSO.

**Contexte d'agence (brique transverse `dz.gam.poste.contexte`, ADR 0005)** : l'identité fournit
un **périmètre d'agences** (mock `poste.contexte` : AGA M. Benzerga + 3 agences). L'**agence active**
est portée par la **session serveur** (`@SessionScope`), jamais par le navigateur. Les autres
fonctions lisent l'agence active via le port d'entrée `AgenceCouranteQuery` et **ne redemandent
jamais** l'agence. SÉCURITÉ : le périmètre est **revérifié côté back** à chaque changement/accès
(hors périmètre → HTTP 403) ; ne jamais faire confiance à l'agence envoyée par le front.
Les modules `cotation`/`dpd` conservent pour l'instant leur identité mono-agence propre (alignement
sur le contexte transverse à prévoir).
