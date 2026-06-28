# ADR 0009 — Déclaration de sinistre (front + DECSIN, PWA offline)

- **Statut** : accepté
- **Date** : 2026-06-27

## Contexte
Nouvelle fonction **mobile-first** à **deux faces** demandant les mêmes pièces (photos +
documents) : l'**AGA** (dans le poste) saisit/contrôle/valide ; le **client** remplit depuis son
téléphone via un lien, **hors-ligne possible**. Le **backend de déclaration existe déjà** (DECSIN,
`api2.gam.dz`), et PROASSUR reste le système de référence du sinistre.

## Décision
1. **Pas de contexte Java** pour cette fonction (contrairement aux autres) : DECSIN est le backend.
   Tout passe par une **couche adaptateur front unique** `shared/decsin/` (port + mock + HTTP réel),
   mutualisée par les deux faces via l'alias Vite `@decsin`. Bascule mock ↔ réel par
   `VITE_DECSIN_MODE`. **Rien en dur** (URL, clé d'API, paramètres → `.env`).
2. **Style** : réutilisation du design system **gam.css** (mêmes tokens que les maquettes), pas de
   Tailwind.
3. **Face AGA** : feature `frontend/src/features/sinistre/` (hérite du contexte d'agence) — onglets
   Nouvelle déclaration (prefill immat, mode saisie/lien, 3 groupes de pièces + masque) et Suivi
   (statuts, **Valider → PROASSUR** qui renvoie le N° de sinistre). Le poste devient **installable** (PWA).
4. **Face client** : **PWA autonome** `modules/declarations-sinistre/` (Vite + vite-plugin-pwa +
   Dexie). 4 écrans (identification double facteur, détails, pièces, confirmation), **offline-first**.
5. **Pièces** : règle de validation **unique** (`shared/decsin/pieces.ts`) — recto+verso constat +
   4 faces véhicule (masque de cadrage = overlay de guidage, **pas de rognage**) ; assurance adverse
   **si tiers**. Deux sources : caméra ou galerie ; compression avant stockage.
6. **Offline & synchro** (client) : Dexie (déclarations + pièces blobs, dossier interne par type),
   `navigator.storage.persist()` (+ alerte si refus), **file de synchro 2 temps** (pièce→référence,
   puis déclaration) déclenchée au retour réseau et à l'ouverture, **idempotente** (`idLocal`),
   statut visible (en attente / synchronisé / échec).
7. **Deux mondes d'authentification distincts** : AGA = SSO/Entra ID + contexte d'agence (interne) ;
   client = lien + double facteur (téléphone + code SMS), session locale persistée, déconnexion =
   purge du local.

## Mise à jour v2 (socle de capture partagé, 5 vues, contrôle)
- **Socle de capture PARTAGÉ** : composants React dans `shared/sinistre-ui/` (alias `@sinistre-ui`)
  réutilisés par l'AGA (poste + mobile) et le client — un seul socle, pas de duplication.
- **Véhicule = 5 vues** avec **sélecteur** et **silhouettes SVG en overlay** (reprises verbatim des
  maquettes ; « droite » = miroir de « gauche »). Les **4 faces sont obligatoires, le TOIT est
  FACULTATIF** (proposé avec silhouette, non bloquant).
- **AGA-mobile** : parcours guidé (stepper détails → capture → validation) dans la feature du poste,
  sans code/SMS (AGA authentifié).
- **Écran de contrôle** (`ApercusControle`) partagé : bandeau de complétude (règle calculée) +
  vignettes + **lightbox**. Côté AGA → page détail (`/declaration-sinistre/:idLocal`, ouverte depuis
  le Suivi) avec **Valider → PROASSUR** (désactivé si incomplet) / **Renvoyer au client**. Côté
  client → écran de récap avant la confirmation.
- **Validation différée** : la capture marche hors-ligne ; le rattachement PROASSUR (N° sinistre)
  requiert le réseau → hors-ligne, la déclaration est « À valider » (drapeau `aRattacher`) et
  **rattachée automatiquement à la reconnexion** (`rattacherDeclarationsEnAttente`).
- **OCR** : point de branchement documenté dans `CaptureDocuments` (pré-remplissage futur) — non implémenté.

## Conséquences
- Bascule vers le **backend réel** sans toucher au domaine : `decsinHttp` remplace `decsinMock`.
- Casse exacte des champs DECSIN, endpoints, clé d'API, mécanique du double facteur et durée de
  session restent **[DSI/métier à confirmer]** ; le mock couvre tout le parcours en attendant.
- Tests Vitest : règle de validation des pièces + synchro 2 temps (ordre + statuts). Pas de test Java.
