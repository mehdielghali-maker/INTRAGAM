# ADR 0011 — Reconnaissance véhicule + lecture de plaque (ANPR, service auto-hébergé remplaçable)

- **Statut** : accepté
- **Date** : 2026-06-28

## Contexte
Sur les photos de véhicule capturées par la déclaration de sinistre (ADR 0009) et la souscription
auto (ADR 0010), on veut **vérifier qu'il s'agit bien d'un véhicule** et **lire le numéro de plaque**,
puis comparer cette plaque à l'**immatriculation du contrat** (pré-remplie depuis PROASSUR) — comme
aide qualité et garde-fou anti-fraude. Atout pour l'Algérie : les plaques sont **numériques**, donc
l'OCR de chiffres est fiable. La capacité doit rester **indépendante** (aucun couplage à PROASSUR,
GAMIA ou un autre service interne) et **remplaçable** (build vs buy : fast-alpr open-source au départ,
bascule possible vers Plate Recognizer sans toucher au reste).

## Décision
1. **Microservice RECO autonome** (`services/reco/`, Python/FastAPI) : `POST /analyser` (multipart
   `photo` + `vue`) → `{ estVehicule, typeVehicule, plaque, confiance, confianceVehicule }` ; `GET
   /health`. Pipeline YOLO (détection « véhicule ? ») + fast-alpr (détection plaque + OCR ONNX). **Rien
   en dur** : seuils et modèles par variables d'environnement (`RECO_*`). Le service **lit** une plaque,
   il ne **juge pas** la conformité (il ne connaît pas le contrat). Image Docker derrière le **profil
   compose `reco`** (opt-in) pour ne pas alourdir le cycle de dev en mode mock.
2. **Contexte hexagonal `dz.gam.poste.reconnaissance`** (pas `dz.gam.intragam.*` : hors du package racine
   `dz.gam.poste`, les beans ne seraient pas component-scannés). Le poste ne dépend que du **port**
   `ReconnaissancePort` (driven, `domain/port/out`) ; record `ResultatReco`. L'implémentation est choisie
   par configuration (ADR 0002).
3. **Adapters mock / réel par `@ConditionalOnProperty`** (clés à plat `reco.*`) : `ReconnaissanceMockAdapter`
   (`reco.mode=mock`, défaut, `matchIfMissing`) pour dev/tests sans dépendance externe ; `RecoHttpAdapter`
   (`reco.mode=http`, `reco.base-url`) appelle le microservice en multipart (RestClient). **Boucle de
   confort** : toute panne du service RECO → **résultat neutre** (jamais bloquant) ; la déclaration n'est
   jamais bloquée par une indisponibilité de reconnaissance.
4. **Comparaison plaque ↔ contrat côté domaine** (`VerificationPlaqueService`, Java pur) → statut
   `CONFORME / NON_CONFORME / NON_LUE / PAS_UN_VEHICULE / VUE_SANS_PLAQUE`. Normalisation des deux côtés
   (majuscules, suppression des non-alphanumériques) avant comparaison. Tolérante : une lecture incertaine
   n'accuse jamais à tort. La **plaque n'est attendue que sur les vues `avant`/`arriere`** (les vues
   latérales/toit → `VUE_SANS_PLAQUE`), aligné sur le service RECO.
5. **Use case + endpoint** : `VerifierVehiculeUseCase` (orchestre lecture → comparaison → décision de
   blocage), exposé par `POST /api/reconnaissance/analyser` (multipart `photo`, `vue` facultatif,
   `immatriculation`) → `ResultatVerificationVehicule { statut, plaqueLue, typeVehicule, confiance,
   estVehicule, bloquant }`. Endpoint **derrière l'authentification** (session requise, ADR 0008). La
   logique métier (comparaison, anti-fraude) reste **serveur**, jamais en TypeScript.
6. **Anti-fraude configurable** (`reco.bloque-non-conforme`, défaut `false`) : si activé, une
   non-concordance plaque côté AGA rend la validation **bloquante** (`bloquant=true`) — sinon simple
   avertissement. Décision portée par `bloqueValidation(statut, estAga, bloqueNonConforme)`.

## Conséquences
- **+** Capacité indépendante et remplaçable : changer de moteur ANPR (fast-alpr → Plate Recognizer, ou
  fine-tuning sur plaques algériennes) ne touche QUE l'adapter, jamais le domaine ni l'UI.
- **+** Mode mock par défaut : `mvn test` et le dev tournent **sans Docker ni modèles ML** ; le profil
  compose `reco` n'est lancé que pour tester `reco.mode=http`.
- **+** En ligne / hors-ligne : la reconnaissance est un **confort** côté serveur ; hors-ligne on capture
  et envoie normalement, l'analyse se fait à la synchronisation. Une panne RECO ne bloque jamais.
- **+** Câblé au front (écrans de contrôle AGA) : composant partagé `@sinistre-ui/VerificationPlaque`
  affiche CONFORME / NON_CONFORME / NON_LUE sous le contrôle de complétude (`DetailControlePage`
  sinistre, `DetailSouscriptionPage`). Il sélectionne la face porteuse de plaque (avant puis arrière),
  mappe les types des deux catalogues (`face_*`/`veh_*` → `avant/arriere/...` par suffixe) et appelle
  `POST /api/reconnaissance/analyser` (session, `credentials: same-origin`). Bandeau de **confort** :
  n'altère jamais la complétude du dossier.
- **−** Reste optionnel : quand `reco.bloque-non-conforme=true`, désactiver effectivement le bouton de
  validation côté front (aujourd'hui `bloquant` est affiché dans le bandeau mais le bouton n'est pas grisé).
- **−** Les photos partent déjà vers DECSIN/SecGam ; le mode http re-analyse l'image une 2ᵉ fois côté
  RECO (on ne redirige pas DECSIN vers RECO). Acceptable pour un confort.
- **−** **Déviation à la règle « 1 IT Testcontainers par fonction »** : RECO n'a **pas de boucle fermée**
  (pas d'événement bus, pas de persistance) — c'est une capacité synchrone. Couverture par **tests
  unitaires purs** (`VerificationPlaqueServiceTest` = logique de comparaison/normalisation/blocage ;
  `ReconnaissanceServiceTest` = orchestration + résultat neutre en panne), sans RabbitMQ/PostgreSQL.
- **−** Réglages terrain (seuils `RECO_SEUIL_*`, fine-tuning) à calibrer sur de vraies photos
  (jour/nuit, plaque sale, angle) — **[DSI/terrain à confirmer]**.
