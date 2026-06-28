# Souscription auto — PWA agent + section poste

Fonction **souscription d'un contrat automobile** (mobile-first), sur le même principe que la
déclaration de sinistre. Inspirée de l'app agent **« UNF Expert GAM »** (analyse de l'APK) :
**login agent → recherche police → produit → capture documents + OCR → enregistrement** (envoi
2 temps pièce→référence puis métadonnées) vers le backend GAM `/SecGam/*`.

## Deux points d'accès
- **Section poste** (`frontend/src/features/souscription/`, alias `@souscription`) : AGA authentifié
  (SSO + contexte d'agence). Recherche police → stepper (produit/assuré → capture → contrôle →
  enregistrer) + suivi + détail.
- **PWA agent autonome** (ce module `modules/souscription-auto/`) : offline-first (Dexie + service
  worker), login agent (+ OTP), installable. Parcours complet capturable hors-ligne.

## Socle & domaine partagés
- **Socle de capture** `shared/sinistre-ui` (`@sinistre-ui`) — générique, piloté par un **catalogue** :
  `PiecesCapture`, `CaptureVehicule` (sélecteur de vues + silhouettes SVG), `ApercusControle`, `Lightbox`.
- **Domaine** `shared/souscription` (`@souscription`) — `souscriptionPort` + `souscriptionMock` /
  `souscriptionHttp` (bascule `VITE_SOUSCRIPTION_MODE`), `catalogueSouscription`, règle `peutEnvoyer`.

## Pièces (catalogue souscription auto)
- **Identité & permis** : CNI recto/verso (oblig.), permis recto (oblig.), permis verso (facultatif).
- **Documents** : carte grise (oblig.), attestation / contrat signé / autre (facultatifs).
- **Photos véhicule** (sélecteur de vues) : avant / arrière / gauche / droite (oblig., silhouette) +
  diagonales / n° châssis / intérieur (facultatif).

## Lancer
```bash
cd frontend && npm install && npm run dev                  # poste :5173 → « Souscription auto »
cd modules/souscription-auto && npm install && npm run dev # PWA agent :5175 (login démo + OTP 0000)
```

## Configuration (`.env.example` → `.env.local`)
| Variable | Rôle | Défaut |
|---|---|---|
| `VITE_SOUSCRIPTION_MODE` | `mock` (dev) ou `real` (backend GAM) | `mock` |
| `VITE_SOUSCRIPTION_BASE_URL` | base GAM | `https://api2.gam.dz/APIS/api/v2/` |
| `VITE_SOUSCRIPTION_API_KEY` / `_HEADER` | clé d'API + en-tête `[DSI à confirmer]` | — / `X-Api-Key` |
| `VITE_SESSION_AGENT_JOURS` | durée session agent offline | `7` |
| `VITE_CODE_PREFIXE` | préfixe référence (`SCR-XXXX-AAAA`) | `SCR` |

## Hors-ligne
Dexie (souscriptions + pièces blobs par type/vue), `navigator.storage.persist()` (+ alerte si refus),
**file de synchro 2 temps** (`postFile` = attachFile + enregistrerAttachment → `enregistrerSouscription`),
déclenchée au retour réseau et à l'ouverture, idempotente (`idLocal`), statut visible.

## OCR
`getOcrData(imageBase64, typeDoc)` = **seam non implémenté** (mock figé). Bouton « Pré-remplir (OCR) ».
Branchement réel ultérieur : `initialiserEntitesOcrGAM` + Dynamsoft.

## Tests
```bash
cd modules/souscription-auto && npm test   # règle de pièces + synchro 2 temps
```

## À confirmer (DSI / métier)
Casse exacte des champs / endpoints `/SecGam`, nom/valeur de la clé d'API, structure du paramètre
`metadata=`, mécanique OTP, liste réelle des `TypeProduit`.
