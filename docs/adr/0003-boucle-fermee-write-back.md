# ADR 0003 — Boucle fermée (write-back vers le système de référence)

- **Statut** : accepté
- **Date** : 2026-06-23

## Contexte

Le poste suit des workflows que l'ERP ne gère pas (ex. le parcours physique d'un
chèque : imprimé, remis à l'agence, remis au bénéficiaire…). Mais l'**aboutissement**
de ce workflow concerne une donnée dont PROASSUR est propriétaire : le règlement.
Si le poste gardait ce résultat pour lui, on créerait une vérité parallèle — un
« shadow ERP » — ce qui est explicitement interdit.

## Décision

**Tout statut terminal produit par un workflow du poste est réécrit vers son système
de référence.**

Pour le suivi des chèques :

- les statuts **Encaissé** et **Retourné** sont terminaux ;
- en atteignant un statut terminal, l'agrégat `DossierCheque` émet un **événement de
  domaine** `ChequeStatutFinaliseEvent` ;
- le service draine cet événement et le publie via le port de sortie
  `PublicationEvenementPort` → message `ChequeStatutFinalise` sur le bus ;
- l'adapter PROASSUR (mock) le consomme et **met à jour le règlement** (ici : log).

Le poste stocke le *suivi* (où en est le chèque), jamais le *règlement* lui-même :
celui-ci reste la propriété de PROASSUR. Une donnée = un seul propriétaire.

## Conséquences

- **+** Pas de divergence durable entre le poste et l'ERP : la boucle se referme.
- **+** L'émission de l'événement est dans le domaine (règle métier), la publication
  est un détail d'infrastructure (port) → testable sans bus.
- **−** Le write-back est asynchrone : il faut prévoir l'idempotence côté ERP et le
  rejeu en cas d'échec (géré par le bus, voir [ADR 0004](0004-choix-bus-rabbitmq.md)).
