# ADR 0004 — Bus d'événements : RabbitMQ ; PostgreSQL pour le seul état du poste

- **Statut** : accepté
- **Date** : 2026-06-23

## Contexte

L'intégration aux systèmes de référence doit être **découplée** : pas d'appel
synchrone en dur. Il faut un mécanisme d'échange asynchrone, avec ré-essai, et une
frontière nette pour substituer les mocks plus tard.

## Décision

### Bus : RabbitMQ

- Un **topic exchange** `gam.poste.events`.
- Routing keys : `cheque.emis` (entrant) et `cheque.statut.finalise` (sortant).
- Queues : `poste.cheque-emis` (consommée par le poste) et `proassur.write-back`
  (consommée par l'adapter PROASSUR mock).
- Sérialisation **JSON** (`Jackson2JsonMessageConverter`) → contrats lisibles,
  indépendants du langage (utile si un consommateur .NET apparaît).
- Fourni en dev via `docker-compose`.

*Pourquoi RabbitMQ et pas Kafka :* volumétrie d'un assureur dommages modérée, besoin
de routing/ré-essai/ack par message plutôt que de rétention de log à fort débit ;
opérabilité plus simple pour une équipe qui reprend le code sans formation lourde.

### Persistance : PostgreSQL, état du poste uniquement

- PostgreSQL stocke **exclusivement** l'état propre au poste : le dossier de suivi et
  son cycle de vie (table `dossier_cheque`).
- **Jamais** une copie du transactionnel des systèmes de référence (montants
  comptables, règlements, tarification…). Ces données sont lues/écrites via les
  ports, pas dupliquées.

## Conséquences

- **+** Producteur et consommateur ne se connaissent pas : remplacer un mock = changer
  qui publie/consomme, sans toucher au métier.
- **+** Ré-essai et acquittement par message → support naturel du write-back fiable.
- **+** Contrats JSON versionnables dans `contracts/`.
- **−** Une dépendance d'infra de plus à exploiter (broker). Acceptable et standard.
- **−** Cohérence à terme (eventual consistency) entre poste et ERP : assumée et
  cadrée par la boucle fermée ([ADR 0003](0003-boucle-fermee-write-back.md)).
