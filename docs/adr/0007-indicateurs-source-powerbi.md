# ADR 0007 — Source des indicateurs (substitut du Cube Power BI)

- **Statut** : accepté
- **Date** : 2026-06-25

## Contexte

Les chiffres affichés (KPI de l'accueil, situation mensuelle du versement) proviendront
d'un **Cube Power BI**, pas encore branché. Il faut pouvoir : (1) avoir des valeurs
**distinctes par agence/sous-agence** pour la démo, et (2) **modifier ces chiffres** en
attendant le branchement réel.

## Décision

1. Les ports existants restent la frontière (ADR 0002) : accueil
   (`IndicateursProassurPort`, `IndicateursSagePort`) et versement
   (`ProductionEncaissePort`, `VersementsBanquePort`).
2. Module **`dz.gam.poste.indicateursmock`** = **substitut du Cube Power BI** : un store
   **persistant** (PostgreSQL) des mesures par agence (`mock_mesures_agence`) et de la
   situation par agence + mois (`mock_situation_mensuelle`). Quatre adapters
   (`*StoreAdapter`) implémentent les ports en lisant ce store.
3. **Seeding automatique** au démarrage à partir du **périmètre du contexte**
   (`IdentitePort.feuilles()`) : valeurs fictives **distinctes par feuille** (facteur par
   agence), idempotent (n'écrase pas une saisie). Toute agence/sous-agence ajoutée en
   config est donc couverte sans code.
4. **API d'administration** `/api/mock/indicateurs` (GET/PUT par agence et par
   agence + mois) pour **lire et modifier** les chiffres.
5. **Bascule Power BI** : remplacer les `*StoreAdapter` par des adapters Power BI (même
   ports), supprimer le store + l'API mock. Le domaine et les écrans ne changent pas.

## Conséquences

- **+** Démo réaliste (chiffres distincts par sous-agence ; consolidé par groupe parlant).
- **+** Édition des chiffres sans redéploiement ; persistance des saisies.
- **+** Point de branchement Power BI clair et isolé (les 4 ports).
- **−** Données de démo en base (tables `mock_*`) à ne pas confondre avec l'état métier ;
  elles disparaîtront avec le branchement réel.
