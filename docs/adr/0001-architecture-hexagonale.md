# ADR 0001 — Architecture hexagonale (ports & adapters)

- **Statut** : accepté
- **Date** : 2026-06-23

## Contexte

Le poste de travail unifié ajoute des fonctionnalités métier *au-dessus* de l'ERP
PROASSUR et d'autres systèmes de référence (Sage, OneBase). Contraintes imposées :

- le domaine métier doit rester **indépendant du framework** (si un EDI impose .NET,
  on rebascule sans réécrire la logique) ;
- les vraies APIs éditeur **n'existent pas encore** : on développe contre des
  interfaces et des mocks ;
- code lisible, repris par un dev Java/React sans formation spécifique.

## Décision

On adopte l'**architecture hexagonale**. Pour chaque fonctionnalité :

```
domain/
  model/    → entités, value objects, règles métier (Java pur, zéro annotation framework)
  port/in/  → use cases (interfaces) = ce que le poste sait faire
  port/out/ → interfaces vers le monde extérieur (persistance, publication d'événements)
  service/  → orchestration des use cases (Java pur)
adapter/
  in/web/        → contrôleurs REST (Spring MVC)
  in/messaging/  → consommateurs RabbitMQ
  out/persistence/ → JPA
  out/messaging/   → publication RabbitMQ
config/    → câblage Spring (le seul endroit qui connaît à la fois domaine et technique)
```

Règle de dépendance : **tout pointe vers le domaine, le domaine ne pointe vers rien.**
`domain/` n'importe ni Spring, ni JPA, ni RabbitMQ.

## Conséquences

- **+** Domaine testable en JUnit pur, sans contexte Spring ni infrastructure.
- **+** Le métier est portable (Java → .NET resterait une réécriture d'adapters, pas du cœur).
- **+** Les systèmes de référence sont accessibles uniquement via des ports → mocks
  triviaux à substituer (voir [ADR 0002](0002-mocks-remplacables.md)).
- **−** Un peu plus de classes (mapping DTO ↔ domaine ↔ entité JPA). Assumé : la
  lisibilité et le découplage priment sur la concision.
