# ADR 0005 — Contexte d'agence & contrôle de périmètre

- **Statut** : accepté
- **Date** : 2026-06-25

## Contexte

Un AGA gère plusieurs agences ; un agent une seule. L'utilisateur doit atterrir
**directement sur l'accueil** (pas d'écran de sélection), choisir/changer d'agence via un
**commutateur global** en haut de l'écran, et tous les écrans doivent hériter de cette
agence active. La liste des agences accessibles relève de l'**identité SSO** (Entra ID,
mockée en dev) et constitue un **périmètre de sécurité**.

## Décision

1. **Brique transverse `dz.gam.poste.contexte`** (hexagonale comme les autres fonctions) :
   - `IdentitePort` (out, mock SSO) fournit l'utilisateur **et son périmètre d'agences** ;
   - `AgenceActiveStore` (out) mémorise l'agence active **dans la session serveur**
     (bean `@SessionScope`) — **jamais** dans le navigateur (pas de `localStorage`) ;
   - `ContexteAgenceService` (domaine pur) sert trois ports d'entrée :
     `ConsulterContexteAgenceUseCase`, `ChangerAgenceActiveUseCase` et la requête
     transverse `AgenceCouranteQuery` consommée par les autres fonctions.
2. **Sécurité non négociable** : l'agence active est **toujours** choisie dans le
   périmètre. Tout changement (et tout `exigerAcces`) **revérifie le code côté back**
   contre le périmètre SSO ; hors périmètre → `AgenceHorsPerimetreException` (**HTTP 403**).
   On ne fait **jamais** confiance à l'agence transmise par le front : les lectures
   dérivent l'agence du **contexte serveur**, pas d'un paramètre client.
3. **Accueil branché** : `TableauBordController` dérive l'agence de `AgenceCouranteQuery` ;
   les ports PROASSUR/Sage prennent le code agence ; le « Bonjour, [agence] » et les KPI
   suivent l'agence active.
4. **Vue consolidée** (« Toutes mes agences ») : disponible dès que le périmètre compte ≥ 2
   agences. C'est une **vue d'ensemble en LECTURE SEULE** — l'accueil agrège les indicateurs
   sur tout le périmètre et fournit la répartition par agence. Toute **action** y est
   interdite : `AgenceCouranteQuery.agencePourAction()` lève `ActionConsolideeInterditeException`
   (HTTP 409), car une action (dépôt, nouvelle demande…) doit toujours viser une agence précise.
   Le mode est porté par la session (sentinelle `CONSOLIDE`), comme l'agence active.

5. **Hiérarchie agences / sous-agences** (2 niveaux). Le périmètre est une liste d'agences
   dont certaines portent des **sous-agences**. L'unité d'**action** est toujours une
   **feuille** (sous-agence, ou agence autonome sans sous-agence). Sélectionner une **agence
   parente** = **vue consolidée (lecture seule)** de ses sous-agences ; « Toutes mes agences »
   = consolidé de tout le périmètre. Règle unifiée : une sélection couvrant **> 1 feuille**
   est en lecture seule (`agencePourAction()` → 409) ; les listes/agrégats portent sur
   `agencesActives()` (les feuilles couvertes). Le consolidé global d'origine n'est qu'un cas
   particulier de cette règle.

## Conséquences

- **+** Une seule source de vérité du périmètre ; rejet centralisé hors périmètre.
- **+** Le front ne stocke rien ; au refresh, le contexte est rechargé du serveur.
- **+** Les futures fonctions (ex. Versement bancaire) héritent de l'agence via
  `AgenceCouranteQuery`, sans redemander l'agence.
- **−** En prod, il faudra mapper les claims du token Entra ID sur `IdentitePort` et
  sécuriser la session (cookie `HttpOnly`/`Secure`, HTTPS).
