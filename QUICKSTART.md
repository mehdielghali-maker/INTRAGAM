# QUICKSTART — installer et tester INTRAGAM en local (collègues)

> Objectif : tout faire tourner **sur votre machine** (aucune donnée ne sort de votre poste —
> l'OCR et la reconnaissance véhicule sont **100 % locaux**) et dérouler les mêmes tests que
> la démo de référence. Durée : ~30 min la première fois (téléchargement des modèles ML).

## 1 · Prérequis (voie simple)

- **Docker Desktop** installé et démarré (Windows/Mac/Linux) — **8 Go de RAM** minimum
  alloués (l'OCR PaddleOCR + YOLO sont gourmands).
- **Git**. C'est tout : ni Java, ni Node, ni Python à installer (tout est conteneurisé).

## 2 · Installation (3 commandes)

```bash
git clone https://github.com/mehdielghali-maker/INTRAGAM.git
cd INTRAGAM
docker compose -f docker-compose.full.yml up --build
```

Premier lancement **long** (build backend Maven + front + image reco avec les modèles
YOLO/plaque/PaddleOCR fr+arabe — tout est mis en cache ensuite). C'est prêt quand le service
`reco` affiche `Modeles charges.` et le backend `Started PosteApplication`.

➡️ Ouvrir **http://localhost:8081**

| Compte | Login / mot de passe | Rôle |
|---|---|---|
| AGA multi-agences (la démo de référence) | `benzerga` / `gam2026` | 5 agences |
| Agent mono-agence | `saidi` / `gam2026` | 1 agence |
| Administrateur | `admin` / `admin` | admin seulement |

*(Optionnel : `cp .env.example .env` pour ajuster ports/variables — les défauts suffisent.)*

## 3 · Les tests de référence

**Reconnaissance véhicule + plaque (RÉELLE)** — menu *Souscription auto* (ou *Déclaration
de sinistre*) → rechercher une police (ex. `AUTO-2026-00123`) → « Je saisis » → étape photos :
- importer la photo d'un **véhicule** → bandeau ✓ « véhicule détecté · plaque lue « … » » ;
- importer une photo **sans véhicule** (clavier…) → bandeau ⚠ « ne semble pas montrer un véhicule » ;
- plaque algérienne 11 chiffres : lecture partielle complétée par des `*` (limite documentée
  des modèles publics — cf. `services/reco/README.md`).

**Attestations + OCR local (RÉEL)** — menu *Attestations* :
1. choisir le **mois de production** (chaque mois = son relevé, rouvert à l'identique) ;
2. photographier/importer une **attestation GAM** → panneau assisté pré-rempli (n° de police
   **15 chiffres**, quittance 8 chiffres, immat, validité, prime) → corriger si besoin →
   **Ajouter au relevé** ;
3. ré-importer la **même** attestation → alerte **doublon** ;
4. couper le réseau (mode avion) → la photo crée quand même une ligne « à lire », relue au
   retour du réseau ;
5. **Valider le relevé du mois** → relevé verrouillé (re-soumission refusée).

**Données de démo par agence** — cotations, accords d'échéancier, versements, chèques et KPIs
sont semés automatiquement pour chaque agence : changer d'**agence active** (sélecteur en haut)
pour voir chaque lot. La vue « Toutes mes agences » est en lecture seule.

## 4 · Tester sur téléphone (caméra ⇒ HTTPS obligatoire)

```bash
winget install Cloudflare.cloudflared        # une fois (Windows ; brew sur Mac)
cloudflared tunnel --url http://localhost:8081
```
Ouvrir l'URL `https://….trycloudflare.com` affichée, sur le téléphone (URL jetable, valable
tant que le tunnel tourne). L'app est **installable** (PWA) : Chrome ⋮ → « Installer ».

## 5 · Dépannage

| Symptôme | Remède |
|---|---|
| Machine légère / build reco trop lourd | `OCR_DOC_ACTIF=false` dans `.env` (garde la plaque, sans PaddleOCR) — ou tout mock : `RECO_MODE=mock`, `OCR_MODE=mock` |
| Réinitialiser les données | `docker compose -f docker-compose.full.yml down -v` puis `up` (reseed automatique) |
| Reprendre après un `git pull` | relancer avec `--build` |
| Port 8081 occupé | `FRONT_PORT=8082` dans `.env` |

## Pour aller plus loin

- **Mode développeur** (coder, hot-reload) : `docs/HANDOFF.md` § « Lancer l'app ».
- Architecture & règles projet : `CLAUDE.md`, `docs/adr/` (ADR 0011 reco, 0013 attestations).
- Détails du service de reconnaissance/OCR : `services/reco/README.md`.
