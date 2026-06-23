# ADR 0002 — Mocks remplaçables (couche anti-corruption)

- **Statut** : accepté
- **Date** : 2026-06-23

## Contexte

PROASSUR, Sage et OneBase sont les systèmes de référence, mais leurs APIs réelles
ne sont **pas encore disponibles**. Il faut pouvoir développer et tester la logique
métier dès maintenant, puis brancher les vraies intégrations **sans toucher au code
métier**.

## Décision

1. **Aucun appel en dur** vers un système de référence depuis le domaine. Toute
   interaction passe par un **port** (interface) défini côté domaine.
2. Les implémentations actuelles sont des **adapters mock** isolés dans le package
   `dz.gam.poste.proassurmock`. Ils simulent le comportement de l'ERP :
   - `ProassurMockPublisher` émet l'événement entrant `ChequeEmis` ;
   - `ProassurMockWriteBackListener` consomme `ChequeStatutFinalise` et **journalise**
     la mise à jour du règlement (write-back simulé).
3. Les échanges se font via des **événements/contrats** (`contracts/`), pas via des
   types internes. Quand la vraie API PROASSUR arrivera, on remplace l'adapter mock
   par un adapter HTTP/SOAP **derrière le même contrat** : le domaine ne change pas.

## Conséquences

- **+** Le métier est développable et testable aujourd'hui.
- **+** Le remplacement d'un mock = nouveau bean adapter, zéro impact sur `domain/`.
- **+** Frontière anti-corruption nette : le vocabulaire de l'ERP ne fuit pas dans le
  domaine du poste.
- **−** Le mock doit rester fidèle au contrat réel ; les schémas de `contracts/`
  servent de référence partagée pour éviter la dérive.
