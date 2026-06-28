# ADR 0012 — Workflow présentiel par défaut + BROUILLON (déclaration & souscription)

- **Statut** : accepté
- **Date** : 2026-06-28

## Contexte
Après test d'INTRAGAM, le workflow de la **déclaration de sinistre** et de la **souscription** partait
d'une mauvaise hypothèse (« trois situations équivalentes », le lien client présenté à égalité avec la
saisie). Le bon modèle métier :
- **Cas par défaut = PRÉSENTIEL** : l'AGA saisit le dossier à son agence quand le client se présente.
- S'il manque des informations, l'AGA **n'est pas bloqué** : il **enregistre en BROUILLON** et peut
  **reprendre / modifier** autant que nécessaire jusqu'à complétude → puis il **VALIDE**.
- **Cas exception = CLIENT À DISTANCE** : seulement si le client ne peut pas se déplacer, l'AGA lui
  envoie un **LIEN** ; le client remplit à distance ; l'AGA **relance** s'il manque des infos ; puis valide.

Le rattachement au système de référence (PROASSUR pour le sinistre, GAM/SecGam pour la souscription)
n'a lieu **qu'à la validation** : un brouillon ne devient un sinistre/contrat qu'à ce moment.

## Décision
1. **Machine à états UNIFIÉE** (socle `shared/dossier/`, alias `@dossier`) partagée par la déclaration
   ET la souscription : `BROUILLON · LIEN_ENVOYE · A_VALIDER · RELANCE · VALIDEE` (remplace l'ancien
   `INCOMPLETE` ; la souscription passe `ENREGISTREE → VALIDEE`). Helpers : `estModifiable` (≠ VALIDEE),
   `estRattachable`/`estVerrouille` (= VALIDEE), `peutValider(statut, complet)` (BROUILLON ou A_VALIDER
   **et** complet). Présentiel : `BROUILLON ⇄ (édite) → A_VALIDER → VALIDEE`. Distant :
   `LIEN_ENVOYE → A_VALIDER → (relance) RELANCE → A_VALIDER → VALIDEE`.
2. **Socle « dossier en cours » FIN** (sans coupler les domaines) : machine à états + **mécanisme
   d'auto-enregistrement débouncé** (`creerAutoEnregistrement`, anti-perte) + interface `BrouillonStore`.
   La **persistance reste dans chaque surface** : `brouillonsLocaux` (localStorage) côté poste pour
   `@decsin` et `@souscription` ; Dexie côté PWA. La **règle de complétude reste paramétrable par
   module** (catalogue `piecesManquantes(pieces, ctx)` existant — inchangé).
3. **BROUILLON = LOCAL-ONLY** : un brouillon vit en local (mock ET réel) et n'est **jamais** envoyé au
   backend avant validation (l'API DECSIN/SecGam ne gère pas forcément les brouillons — **[DSI à
   confirmer]**). `saveDeclaration`/l'adapter ne **forcent plus** de statut ; le front porte la machine
   à états. La complétude conditionne **uniquement** la validation, **jamais** l'enregistrement brouillon.
4. **Auto-enregistrement continu débouncé** (~800 ms) en plus du bouton explicite « Enregistrer en
   brouillon » : aucune saisie (champs **et** pièces) n'est perdue. Les pièces sont persistées
   immédiatement (localStorage/Dexie) ; les champs sont sauvés en différé.
5. **Écrans** (poste + PWA) recadrés : « Saisie par l'AGA (présentiel) » mise en avant, « Envoyer un
   lien (à distance) » en action secondaire ; bas d'écran « Enregistrer en brouillon » (toujours actif)
   + « Valider » (gated, avec rappel du manquant) ; suivi listant les brouillons + filtres + « Reprendre » ;
   détail « Reprendre la saisie » (BROUILLON) ou « Valider »/« Renvoyer → RELANCE » (A_VALIDER).
6. **Offline** : un brouillon créé hors-ligne se synchronise **en tant que brouillon** (`statutSync =
   brouillon`, hors file) ; seule la **validation** bascule en `en_attente` → rattachement à la
   reconnexion. Idempotence par `idLocal` conservée.
7. **Souscription — face client à distance** (le « lien complet ») : la souscription n'avait aucune
   surface client. Ajout d'une **auth client** (`loginClient`, téléphone + OTP) et d'une **PWA cliente**
   `modules/souscription-client/` (offline, lien + double facteur), symétrique de la PWA déclaration ;
   action AGA « Envoyer un lien au client » côté poste.

## Conséquences
- **+** Un seul socle d'états/brouillon pour les deux domaines (et extensible aux futurs : DPD, etc.).
  Chaque module garde ses champs, pièces obligatoires et ports.
- **+** Plus de perte de saisie (auto-save) ; reprise simple ; le présentiel (cas réel majoritaire) est
  le chemin par défaut.
- **+** Rien n'est rattaché à PROASSUR/GAM avant validation — conforme à la « boucle fermée » (ADR 0003).
- **−** Le brouillon est **local jusqu'à validation** (choix assumé, faute de brouillon serveur DECSIN —
  **[DSI à confirmer]**) ; un brouillon présentiel n'est pas visible cross-appareil (acceptable : le
  présentiel se fait sur le poste de l'AGA ; le distant passe, lui, par le backend dès `LIEN_ENVOYE`).
- **−** RECO, masques SVG, détail/contrôle, offline et mock **inchangés** (non cassés) ; seuls le modèle,
  les états, la persistance brouillon et le cadrage des écrans évoluent.
- Reste optionnel : brancher un endpoint de **brouillon serveur** quand la DSI le confirmera (le socle
  `BrouillonStore` est prêt à recevoir une implémentation distante).
