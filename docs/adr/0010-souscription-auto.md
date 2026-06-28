# ADR 0010 — Souscription auto (socle de capture générique + adaptateur GAM, PWA agent)

- **Statut** : accepté
- **Date** : 2026-06-28

## Contexte
Ajout d'une fonction **souscription d'un contrat automobile**, sur le même principe que la
déclaration de sinistre. Référence : l'app agent **« UNF Expert GAM »** (Expo/React Native, analyse
de l'APK) — parcours : **login agent (+ OTP)** → **recherche police** (branche / n° / client) →
**type de produit** → **assistant à étapes** → **capture documents + OCR** (CNI, permis, carte grise,
photos véhicule) → **enregistrement** (envoi 2 temps pièce→référence puis métadonnées) vers le
backend GAM `/SecGam/*` (`api2.gam.dz/APIS/api/v2`).

## Décision
1. **Socle de capture rendu GÉNÉRIQUE** (`shared/sinistre-ui`, alias `@sinistre-ui`) : piloté par un
   **Catalogue** (`catalogue.ts`) — groupes, définitions de pièces, silhouettes, règle de complétude.
   La déclaration fournit `catalogueDeclaration` (5 vues, tiers) ; la souscription `catalogueSouscription`.
   Les composants (`PiecesCapture`, `CaptureVehicule`, `CaptureDocuments`, `ApercusControle`) ne dépendent
   plus d'un domaine. Les silhouettes des 4 faces sont réutilisées (verbatim maquette).
2. **Nouveau domaine `shared/souscription`** (alias `@souscription`), calqué sur `@decsin` : port
   (`loginAgent`, `rechercheEntite`, `listePreEntites`, `getOcrData` [seam], `postFile`,
   `enregistrerSouscription`), **mock** (dev) / **HTTP réel** (`/SecGam/*`), bascule `VITE_SOUSCRIPTION_MODE`.
   **Rien en dur** (base URL, clé d'API → `.env`).
3. **Produit AUTO** uniquement (`TypeProduit='AUTO'`), extensible. **Pièces obligatoires** : CNI
   recto/verso + permis (recto) + carte grise + **4 faces véhicule** ; vues complémentaires
   (diagonales, VIN, intérieur) et permis verso **facultatifs**. Règle calculée unique (`peutEnvoyer`).
4. **Deux points d'accès** : (a) **section poste** `features/souscription` (AGA authentifié, contexte
   d'agence) — recherche → stepper (produit/assuré → capture → contrôle → enregistrer) + suivi + détail ;
   (b) **PWA agent autonome** `modules/souscription-auto` (offline-first : Dexie + vite-plugin-pwa),
   login agent (+ OTP) + session locale, **file de synchro 2 temps**, installable.
5. **OCR** : `getOcrData(imageBase64, typeDoc)` = **seam non implémenté** (mock figé pré-remplissant
   prénom/nom/numéro). Bouton « Pré-remplir (OCR) » côté UI ; branchement réel ultérieur
   (`initialiserEntitesOcrGAM` + Dynamsoft) sans impact UI.
6. **Auth** : monde **agent** (login/mdp + OTP), distinct du monde **client** (déclaration) — à ne pas
   mélanger.

## Conséquences
- Le socle de capture est désormais réutilisable par tout futur parcours (un catalogue suffit).
- Bascule vers le backend réel sans toucher l'UI (`souscriptionHttp`). Casse exacte des champs DECSIN/
  SecGam, clé d'API, structure `metadata=`, mécanique OTP restent **[DSI à confirmer]** ; le mock couvre
  le parcours complet.
- Tests Vitest : règle de pièces souscription + synchro 2 temps (module) ; **non-régression déclaration**
  vérifiée (socle généralisé) — 10 tests déclaration + 6 tests souscription verts.
- Note technique : les `.tsx` de `shared/` résolvent React via `paths` (`@types/react`) dans les tsconfig.
