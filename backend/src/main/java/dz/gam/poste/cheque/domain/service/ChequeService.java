package dz.gam.poste.cheque.domain.service;

import dz.gam.poste.cheque.domain.event.ChequeStatutFinaliseEvent;
import dz.gam.poste.cheque.domain.model.DossierCheque;
import dz.gam.poste.cheque.domain.model.DossierIntrouvableException;
import dz.gam.poste.cheque.domain.model.Montant;
import dz.gam.poste.cheque.domain.model.ReferenceCheque;
import dz.gam.poste.cheque.domain.model.StatutCheque;
import dz.gam.poste.cheque.domain.port.in.ChequeEmisCommand;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.EnregistrerChequeEmisUseCase;
import dz.gam.poste.cheque.domain.port.in.FaireAvancerStatutUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.cheque.domain.port.out.DossierChequeRepository;
import dz.gam.poste.cheque.domain.port.out.PublicationEvenementPort;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Service applicatif du périmètre « Suivi des chèques » : il orchestre les use cases
 * en s'appuyant sur les ports de sortie. Java pur, aucune dépendance framework — son
 * câblage Spring est fait dans la couche {@code config}.
 */
public class ChequeService implements EnregistrerChequeEmisUseCase, FaireAvancerStatutUseCase, ConsulterDossiersUseCase {

    private final DossierChequeRepository repository;
    private final PublicationEvenementPort publication;
    private final Clock horloge;

    public ChequeService(DossierChequeRepository repository, PublicationEvenementPort publication, Clock horloge) {
        this.repository = repository;
        this.publication = publication;
        this.horloge = horloge;
    }

    @Override
    public DossierCheque enregistrer(ChequeEmisCommand commande) {
        ReferenceCheque reference = new ReferenceCheque(commande.reference());

        // Idempotence : le bus peut redélivrer ChequeEmis. On n'ouvre pas deux dossiers.
        return repository.trouverParReference(reference)
                .orElseGet(() -> {
                    Montant montant = new Montant(commande.montant(),
                            commande.devise() == null ? "DZD" : commande.devise());
                    DossierCheque dossier = DossierCheque.creerDepuisEmission(
                            reference, montant, commande.beneficiaire(), commande.agence(),
                            commande.dateEmission(), horloge.instant());
                    return repository.enregistrer(dossier);
                });
    }

    @Override
    public DossierCheque faireAvancer(UUID idDossier, StatutCheque statutCible) {
        DossierCheque dossier = repository.trouverParId(idDossier)
                .orElseThrow(() -> new DossierIntrouvableException(idDossier));

        dossier.faireAvancerVers(statutCible, horloge.instant());
        DossierCheque enregistre = repository.enregistrer(dossier);

        // Boucle fermée : on publie les événements terminaux puis on les vide.
        // NB : pour une atomicité stricte DB + bus, on introduirait un outbox
        // transactionnel ; hors périmètre de cette tranche.
        publierEvenements(enregistre);
        return enregistre;
    }

    @Override
    public List<DossierCheque> lister(FiltreDossier filtre) {
        return repository.lister(filtre);
    }

    @Override
    public DossierCheque obtenir(UUID idDossier) {
        return repository.trouverParId(idDossier)
                .orElseThrow(() -> new DossierIntrouvableException(idDossier));
    }

    private void publierEvenements(DossierCheque dossier) {
        for (ChequeStatutFinaliseEvent evenement : dossier.evenementsNonPublies()) {
            publication.publier(evenement);
        }
        dossier.viderEvenements();
    }
}
