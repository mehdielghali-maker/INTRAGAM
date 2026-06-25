# Point de reprise (handoff)

> Document de passation pour reprendre le travail dans une nouvelle session.
> Voir aussi `CLAUDE.md` (règles projet) et `docs/adr/` (décisions).

## Où on en est
- **Branche** : `feat/contexte-agence-versement` (PAS `main`).
- **Remote** : `origin` = `https://github.com/mehdielghali-maker/INTRAGAM.git` (compte
  `mehdielghali-maker`, authentifié via GitHub CLI `gh`). **PR ouverte : INTRAGAM#1**.
  *(Le repo client `mtirchi/INTRAGAM` était inaccessible → repo créé sous le compte connecté.)*
- **Tests** : `mvn verify` vert = **64 unitaires + 4 boucles d'intégration** Testcontainers.
- Tout est committé et poussé.

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

## Ce qui a été fait cette session (résumé des commits)
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
- **Profils / connexion** : `GET /api/mock/identite` (liste + actif) ·
  `POST /api/mock/identite/actif {"identifiant":"…"}` (activer un AGA pour la session).
- **Admin profils** : `GET/POST /api/admin/profils`, `PUT/DELETE /api/admin/profils/{id}`
  (corps : `{identifiant, nomAffiche, profil:"AGA|AGENT", agences:[{code,nom}], modules:[ids]}`).
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
- Brancher l'**adapter Power BI** réel (remplacer les `*StoreAdapter` d'`indicateursmock`, mêmes ports).
- **Auth Entra ID** réelle (le contexte est prêt à recevoir utilisateur + périmètre + modules depuis les claims).
- Gérer les **chiffres par agence dans le dashboard Admin** (aujourd'hui via `/api/mock/indicateurs`).
- Implémenter les **fonctions placeholder** (Dépôt SF, Attestations, etc.).
- **Merger la PR** INTRAGAM#1.
