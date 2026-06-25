# ADR 0006 — Versement bancaire (preuve de paiement)

- **Statut** : accepté
- **Date** : 2026-06-25

## Contexte

L'agence (ou l'AGA pour l'agence active) doit déposer le reçu de versement justifiant
l'encaissement des primes émises sur un mois. La pièce part au BPM et est rattachée à la
situation financière (agence + mois). Le poste rattache et suit ; il ne recalcule pas la
comptabilité (ADR 0003).

## Décision

1. **Module hexagonal `dz.gam.poste.versement`** calqué sur les fonctions existantes :
   agrégat `Versement` (cycle Brouillon → Déposé → En contrôle → Validé / Rejeté), service
   de domaine pur, adapters in/out, persistance JPA, config en `application.yml`.
2. **Héritage du contexte d'agence (ADR 0005)** : pas de sélecteur dans l'écran. Le
   contrôleur dérive l'agence active et le créateur du contexte de session ; la liste et la
   situation sont bornées à l'agence active. L'agence n'est jamais reçue du client.
3. **Ports MOCK derrière contrats** :
   - PROASSUR (`ProductionEncaissePort`) : production émise + encaissé du mois/agence ;
   - Sage (`VersementsBanquePort`) : déjà versé en banque du mois/agence ;
   - GED OneBase (`GedVersementPort`) : dépôt du reçu → référence (jamais le binaire) ;
   - BPM (`PublicationVersementPort` + `VersementStatutListener` + `BpmOneBaseMock`).
4. **Boucle fermée (ADR 0003)** : « Soumettre au BPM » publie `VersementBancaireDepose`
   (RK `versement.depose`) ; le BPM (mock) renvoie les statuts (RK `versement.statut`)
   appliqués par le poste. Reçu **obligatoire** et **montant strictement positif** à la
   soumission (invariants du domaine).
5. **Cohérence de l'écart (ADR 0005)** : le « Reste à régulariser » = encaissé − versé est
   calculé par le composant partagé `shared.regularisation.EcartRegularisation`, le MÊME que
   le KPI « Écart à régulariser » de l'accueil — une seule définition, pas de divergence.
6. **Capture mobile** : le reçu se prend en photo ou se joint en fichier (multi-pages),
   avec compression d'image côté client ; seules les références (noms → GED) transitent.

## Conséquences

- **+** Réutilisation maximale des patterns (cotation/dpd) ; suivi BPM homogène.
- **+** Reste à régulariser garanti identique à l'accueil (source unique).
- **+** Compteur de navigation « Versement bancaire » réel (versements en cours).
- **−** Le BPM/PROASSUR/Sage/OneBase réels restent à brancher derrière les ports ; le
  binaire de la pièce devra réellement transiter vers la GED (au-delà du nom, en prod).
