# ADR 0013 — Attestations : relevé mensuel de production + OCR local de documents

Date : 2026-07-02 · Statut : accepté

## Contexte

L'AGA déclare chaque mois sa production : la liste des attestations d'assurance émises. La saisie
manuelle est lente et source d'erreurs. On veut : choisir un **mois de production**, **photographier**
chaque attestation, la faire lire par un **OCR strictement local** (données d'assurance = données
personnelles — rien ne sort vers le cloud), et **accumuler** les lignes dans un relevé mensuel,
enregistré au fil de l'eau et **soumis** (PROASSUR/Sage) en fin de mois.

L'attestation GAM est **bilingue français/arabe** : le numéro de police (toujours **15 chiffres**)
apparaît **deux fois** (sous le libellé arabe « رقم عقد التأمين » en tête du certificat, et sur la
ligne « Police N° » de la quittance). Le grand numéro rouge « N° » (8 chiffres) est le **numéro de
quittance** — piège classique à ne pas confondre avec la police.

## Décision

**Même patron que RECO (ADR 0011)** : capacité indépendante et remplaçable, derrière un port.

1. **Service OCR-DOC mutualisé** dans `services/reco/` (arbitrage validé) : PaddleOCR (fr + arabic),
   endpoint `POST /lire-attestation`, modèles **baked dans l'image au build** → aucun appel sortant à
   l'exécution. Désactivable (`OCR_DOC_ACTIF=false`) ; son échec de chargement ne casse jamais
   `/analyser`. Règles d'extraction dans un module **pur et isolé** (`attestation_regles.py`,
   libellés/regex/seuils surchargeables par env, testé sans PaddleOCR) : police = 15 chiffres exigés
   en **double occurrence concordante** (sinon `a_verifier`, confiance plafonnée — le numéro est
   renvoyé pour correction, mode assisté) ; la quittance (8 chiffres) sert de **repère négatif** ;
   dédoublonnage par position des deux lecteurs (une occurrence physique ≠ une concordance).
2. **Backend** : contexte hexagonal `dz.gam.poste.attestation` — port `LectureDocumentPort`
   (mock défaut / http via `ocr.mode`, adapter HTTP reprenant les acquis durs de RECO : HTTP/1.1
   forcé, multipart manuel, timeouts, **panne → résultat neutre jamais bloquant**) ; agrégat
   `ReleveProduction` (un lot par **agence + mois**, référence `PROD-{mois}-{agence}`, doublons de
   police rejetés, re-soumission → 409) ; port `ProductionPort` (mock « production validée » —
   endpoint réel PROASSUR/Sage/DECSIN **[À CONFIRMER DSI]**) ; persistance du relevé **validé**
   (payloadJson, pattern DPD). **Arbitrage : collecte seule** — le seam de vérification de chaque
   police contre PROASSUR est prévu mais non branché.
3. **Front poste** `features/attestation/` (mobile-first, maquette reproduite) : sélecteur de mois
   (ouvre/rouvre le lot **agence active|mois**), capture (caméra mobile / import PC), **mode
   assisté** (l'OCR pré-remplit, l'agent confirme ou corrige — il ne décide jamais seul), tableau
   accumulatif (doublon de police signalé), **relevé ouvert aux ajouts jusqu'à la validation, puis
   verrouillé**. Consolidé = lecture seule (ADR 0005).
4. **Hors-ligne** : Dexie `gam-attestations` (persist() au démarrage) ; la capture crée la ligne
   **immédiatement** (statut `a_lire`), l'OCR passe par une **file idempotente** exécutée à la
   reconnexion ; la validation hors-ligne passe `A_VALIDER` et se soumet à la reconnexion.
   Un brouillon n'est **jamais** envoyé au backend (ADR 0012).

## Conséquences

- + Saisie mensuelle accélérée, zéro donnée personnelle hors des murs, OCR remplaçable (port).
- + Les règles d'extraction sont de la configuration, ajustables si le gabarit varie par branche.
- − PaddleOCR alourdit l'image reco (~2 Go) et sa RAM — `OCR_DOC_ACTIF=false` en environnement
  contraint ; option service jumeau si besoin.
- − L'endpoint réel de soumission de la production reste à confirmer par la DSI (mock en place).
