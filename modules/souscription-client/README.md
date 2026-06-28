# Souscription auto — PWA cliente (face distante)

Face **client** de la souscription auto, utilisée **en exception** : quand le client ne peut pas se
déplacer en agence, l'AGA lui envoie un **lien** (depuis le poste, onglet « Nouvelle souscription » →
« Envoyer un lien »). Le client finalise sa souscription depuis son téléphone, **hors-ligne possible**,
puis la souscription est synchronisée et revient à l'AGA **« à valider »**.

Le mode normal reste **présentiel** (l'AGA saisit en agence, cf. `frontend/src/features/souscription/`).
Cette PWA est le **repli distant**, symétrique de la PWA cliente de déclaration
(`modules/declarations-sinistre/`).

## Parcours
1. **Identité** : ouverture du lien (`?reference=SCR-XXXX-AAAA`) + double facteur (téléphone + code SMS ;
   démo `0000`).
2. **Assuré & véhicule** : champs pré-remplis depuis la souscription initialisée par l'AGA.
3. **Pièces** : CNI + permis + carte grise + 4 faces (socle de capture partagé `@sinistre-ui`).
4. **Vérification** : contrôle de complétude.
5. **Envoi** : la souscription passe **`A_VALIDER`** et part en synchro (2 temps). **Rien n'est validé
   côté GAM** : l'AGA contrôle puis valide (modèle `@dossier`).

## Hors-ligne & anti-perte
- Service worker (Workbox) + manifeste **installable**.
- **Dexie (IndexedDB)** : souscription + pièces (blobs) persistées localement.
- **Auto-enregistrement débouncé** (socle `@dossier`) : aucune saisie perdue, même sans réseau.
- **File de synchro 2 temps** (pièce → référence, puis métadonnées) déclenchée au retour réseau.
  Un brouillon (`statutSync = brouillon`) n'est **jamais** envoyé ; seul l'envoi (`en_attente`) synchronise.

## Lancer
```bash
cd modules/souscription-client && npm install && npm run dev
# http://localhost:5176/?reference=SCR-XXXX-AAAA   (OTP démo : 0000)
```
Le lien est généré par l'AGA (poste) ; URL configurable via `VITE_SOUSCRIPTION_CLIENT_URL` (défaut
`http://localhost:5176`). Couche partagée : `shared/souscription/` (`@souscription`, bascule
`VITE_SOUSCRIPTION_MODE`). Détails de config : `.env.example`.

## Auth — deux mondes distincts
- **Agent** (`modules/souscription-auto/`) : login agent + OTP, app autonome de terrain.
- **Client** (cette app) : lien + double facteur (téléphone + code), session locale ; déconnexion = purge.
