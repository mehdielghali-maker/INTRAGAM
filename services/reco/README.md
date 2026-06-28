# Service RECO — Reconnaissance de véhicule + lecture de plaque

Solution **indépendante et auto-hébergée** pour INTRAGAM : sur les photos de
véhicule, elle (1) vérifie qu'il s'agit bien d'un véhicule et (2) lit le numéro
de plaque. INTRAGAM compare ensuite la plaque lue à l'immatriculation du contrat.

Ce service est **autonome** : aucun couplage avec PROASSUR, GAMIA ou un autre
service interne. On peut le remplacer par un autre moteur sans toucher au reste,
car INTRAGAM ne parle qu'au **port `reconnaissance`**.

---

## 1. Quelle brique de reconnaissance ? (build vs buy)

| Option | Type | Coût | Pour qui |
|---|---|---|---|
| **fast-alpr** (retenu ici) | Open-source, ONNX, auto-hébergé | Gratuit | Indépendance totale, données qui ne sortent pas |
| **Plate Recognizer** (SDK Docker on-premise) | Tiers, clé-en-main | Payant | Précision maximale immédiate, support |
| **OpenALPR** (open-source historique) | Open-source C++ | Gratuit | Existant, mais moins à jour |

**Recommandation** : démarrer avec **fast-alpr** (gratuit, auto-hébergé, données
internes). Atout pour l'Algérie : les plaques sont **numériques** → l'OCR de
chiffres est plus fiable. Si la précision terrain est insuffisante, deux leviers :
**(a)** un fine-tuning du détecteur/OCR sur un jeu de plaques algériennes, ou
**(b)** basculer l'adapter vers **Plate Recognizer** (même port `reconnaissance`,
on ne change que l'implémentation).

La détection « est-ce un véhicule ? » utilise **YOLO** (ultralytics), qui
reconnaît voiture / moto / bus / camion sans entraînement.

---

## 2. Lancer le service

### Docker (recommandé)
```bash
docker build -t reco-service .
docker run -p 8088:8088 reco-service
```

### En local (dev)
```bash
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8088
```

Les modèles se téléchargent au build (ou au premier démarrage). Une fois en
cache, le service démarre **sans réseau**.

---

## 3. API

`POST /analyser` — multipart : `photo` (fichier image), `vue` (optionnel :
`avant` / `arriere` / `gauche` / `droite` / `toit`).

```bash
curl -X POST http://localhost:8088/analyser \
  -F "photo=@avant.jpg" -F "vue=avant"
```

Réponse :
```json
{
  "estVehicule": true,
  "typeVehicule": "voiture",
  "plaque": "0987611616",
  "confiance": 0.91,
  "confianceVehicule": 0.97,
  "vue": "avant"
}
```

- La **plaque n'est lue que sur `avant` / `arriere`** (les autres vues :
  détection de véhicule seule).
- Si la lecture est trop incertaine (< seuil), `plaque` vaut `null` : on
  préfère **ne rien affirmer** plutôt que livrer une fausse plaque.

`GET /health` — sonde de disponibilité.

### Réglages (variables d'environnement)
| Variable | Défaut | Rôle |
|---|---|---|
| `RECO_SEUIL_VEHICULE` | `0.45` | Confiance mini pour déclarer « véhicule » |
| `RECO_SEUIL_PLAQUE` | `0.35` | En dessous, la plaque est jugée non lue |
| `RECO_YOLO_MODEL` | `yolov8n.pt` | Modèle détection véhicule (n=rapide) |
| `RECO_DET_MODEL` | `yolo-v9-t-384-license-plate-end2end` | Détecteur de plaque |
| `RECO_OCR_MODEL` | `cct-xs-v2-global-model` | OCR de plaque |

---

## 4. Branchement INTRAGAM (Spring Boot) — contexte `dz.gam.poste.reconnaissance`

Le branchement vit dans le backend (`backend/src/main/java/dz/gam/poste/reconnaissance/`),
en architecture hexagonale (ADR 0011) :

- **`domain/port/out/ReconnaissancePort.java`** — l'interface (le reste du poste n'utilise QUE ça)
  + le record `ResultatReco`.
- **`adapter/out/reco/RecoHttpAdapter.java`** — appelle ce service (`reco.mode=http`). Si le
  service est indisponible, renvoie un résultat neutre : **la déclaration n'est jamais bloquée**
  par une panne de reconnaissance.
- **`adapter/out/reco/ReconnaissanceMockAdapter.java`** — mock par défaut (dev/tests, `reco.mode=mock`).
- **`domain/service/VerificationPlaqueService.java`** — compare la plaque lue à l'immatriculation
  du contrat (CONFORME / NON_CONFORME / NON_LUE / PAS_UN_VEHICULE / VUE_SANS_PLAQUE).
- **`domain/service/ReconnaissanceService.java`** + **`domain/port/in/VerifierVehiculeUseCase.java`** —
  orchestrent lecture + comparaison + décision de blocage.
- **`adapter/in/web/ReconnaissanceController.java`** — expose `POST /api/reconnaissance/analyser`
  (multipart : `photo`, `vue` facultatif, `immatriculation`) → `{ statut, plaqueLue, typeVehicule,
  confiance, estVehicule, bloquant }`. Endpoint protégé par l'authentification (session requise).

`application.yml` (clés à plat `reco.*`, défaut = mock) :
```yaml
reco:
  mode: mock            # "mock" en dev, "http" en prod
  base-url: http://reco-service:8088   # http://localhost:8088 si le backend tourne en natif
  bloque-non-conforme: false   # true = non-concordance bloquante côté AGA (anti-fraude)
```

### Où se fait quoi
- Le **service RECO** lit une plaque ; il ne connaît pas le contrat.
- **INTRAGAM** (back-end) appelle le port, puis compare la plaque à
  l'immatriculation PROASSUR via `VerificationPlaqueService`, et décide de
  l'affichage (confirmation verte / avertissement) et d'un éventuel blocage.

---

## 5. En ligne / hors-ligne

La reconnaissance tourne **côté serveur**. En ligne : analyse à la prise,
résultat affiché. Hors-ligne : la photo est capturée et stockée normalement,
l'analyse + la comparaison se font **à la synchronisation**. L'absence de réseau
(ou du service) ne bloque **jamais** la capture ni la déclaration.

---

## 6. Précision sur les plaques algériennes

Format numérique `NNNNN NNN NN` (série · type/année · code wilaya). La
normalisation (`VerificationPlaqueService`) retire espaces/tirets des deux côtés
avant comparaison. Pour gagner en précision terrain, prévoir un petit jeu de
photos réelles (jour/nuit, plaque sale, angle) pour calibrer les seuils, puis
un fine-tuning si nécessaire.
