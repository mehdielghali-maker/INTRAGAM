package dz.gam.poste.cotation.domain.service;

import dz.gam.poste.cotation.config.CotationProperties;
import dz.gam.poste.cotation.domain.event.DemandeCotationEmiseEvent;
import dz.gam.poste.cotation.domain.model.ContexteCotation;
import dz.gam.poste.cotation.domain.model.DemandeCotation;
import dz.gam.poste.cotation.domain.model.DemandeIntrouvableException;
import dz.gam.poste.cotation.domain.model.IdentiteAgence;
import dz.gam.poste.cotation.domain.model.PieceJointe;
import dz.gam.poste.cotation.domain.model.ReferenceDemande;
import dz.gam.poste.cotation.domain.model.Souscripteur;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import dz.gam.poste.cotation.domain.port.in.ClotureDemandeUseCase;
import dz.gam.poste.cotation.domain.port.in.ConsulterContexteUseCase;
import dz.gam.poste.cotation.domain.port.in.ConsulterDemandesUseCase;
import dz.gam.poste.cotation.domain.port.in.CreerDemandeCommand;
import dz.gam.poste.cotation.domain.port.in.EnregistrerDemandeUseCase;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
import dz.gam.poste.cotation.domain.port.in.RetourCentralUseCase;
import dz.gam.poste.cotation.domain.port.out.DemandeCotationRepository;
import dz.gam.poste.cotation.domain.port.out.GedOneBasePort;
import dz.gam.poste.cotation.domain.port.out.IdentiteAgencePort;
import dz.gam.poste.cotation.domain.port.out.PublicationCotationPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service applicatif de la « Demande de cotation ». Java pur. Orchestre la demande
 * vers le central et applique ses retours ; ne tarifie jamais et ne réplique pas le
 * contenu du devis.
 */
public class CotationService implements EnregistrerDemandeUseCase, RetourCentralUseCase,
        ClotureDemandeUseCase, ConsulterDemandesUseCase, ConsulterContexteUseCase {

    private final DemandeCotationRepository repository;
    private final PublicationCotationPort publication;
    private final IdentiteAgencePort identite;
    private final GedOneBasePort ged;
    private final CotationProperties properties;
    private final Clock horloge;

    public CotationService(DemandeCotationRepository repository, PublicationCotationPort publication,
                           IdentiteAgencePort identite, GedOneBasePort ged,
                           CotationProperties properties, Clock horloge) {
        this.repository = repository;
        this.publication = publication;
        this.identite = identite;
        this.ged = ged;
        this.properties = properties;
        this.horloge = horloge;
    }

    @Override
    public DemandeCotation enregistrerBrouillon(CreerDemandeCommand commande) {
        return repository.enregistrer(construireBrouillon(commande));
    }

    @Override
    public DemandeCotation envoyer(CreerDemandeCommand commande) {
        DemandeCotation demande = construireBrouillon(commande);
        demande.envoyer(genererReference(), horloge.instant());
        DemandeCotation enregistree = repository.enregistrer(demande);
        publierEvenements(demande);
        return enregistree;
    }

    @Override
    public DemandeCotation envoyerBrouillon(UUID idDemande) {
        DemandeCotation demande = charger(idDemande);
        demande.envoyer(genererReference(), horloge.instant());
        DemandeCotation enregistree = repository.enregistrer(demande);
        publierEvenements(demande);
        return enregistree;
    }

    @Override
    public void prendreEnCharge(String reference, String nomSouscripteur) {
        DemandeCotation demande = chargerParReference(reference);
        demande.prendreEnCharge(new Souscripteur(nomSouscripteur), horloge.instant());
        repository.enregistrer(demande);
    }

    @Override
    public void finaliser(String reference, String numeroProposition, String referenceDevis) {
        DemandeCotation demande = chargerParReference(reference);
        demande.finaliser(numeroProposition, referenceDevis, horloge.instant());
        repository.enregistrer(demande);
    }

    @Override
    public void marquerAffaireGagnee(String reference, String numeroPolice) {
        DemandeCotation demande = chargerParReference(reference);
        demande.marquerAffaireGagnee(numeroPolice, horloge.instant());
        repository.enregistrer(demande);
    }

    @Override
    public DemandeCotation marquerSansSuite(UUID idDemande, String motif) {
        DemandeCotation demande = charger(idDemande);
        demande.marquerSansSuite(motif, horloge.instant());
        return repository.enregistrer(demande);
    }

    @Override
    public List<DemandeCotation> lister(FiltreDemande filtre) {
        return repository.lister(filtre);
    }

    @Override
    public DemandeCotation obtenir(UUID idDemande) {
        return charger(idDemande);
    }

    @Override
    public long compterActives() {
        return repository.lister(FiltreDemande.aucun()).stream()
                .filter(d -> d.statut().estActive())
                .count();
    }

    @Override
    public ContexteCotation contexte() {
        IdentiteAgence id = identite.identiteCourante();
        Map<String, String> libelles = new LinkedHashMap<>();
        for (StatutCotation statut : StatutCotation.values()) {
            libelles.put(statut.name(), properties.libelles().getOrDefault(statut, statut.name()));
        }
        return new ContexteCotation(id, List.copyOf(properties.branches()), libelles);
    }

    private DemandeCotation construireBrouillon(CreerDemandeCommand commande) {
        IdentiteAgence id = identite.identiteCourante();
        List<PieceJointe> pieces = new ArrayList<>();
        if (commande.nomsFichiers() != null) {
            for (String nom : commande.nomsFichiers()) {
                if (nom != null && !nom.isBlank()) {
                    pieces.add(ged.deposer(nom)); // dépose en GED OneBase, ne garde que la référence
                }
            }
        }
        return DemandeCotation.creerBrouillon(commande.objet(), commande.nomProspect(), commande.numeroPolice(),
                commande.commentaire(), id.codeAgence(), id.directionRegionale(), id.utilisateur(),
                pieces, horloge.instant());
    }

    private ReferenceDemande genererReference() {
        long numero = properties.referenceSeed() + repository.compterReferencees() + 1;
        int annee = LocalDate.now(horloge).getYear();
        return new ReferenceDemande("%s-%d-%04d".formatted(properties.prefixeReference(), annee, numero));
    }

    private DemandeCotation charger(UUID id) {
        return repository.trouverParId(id)
                .orElseThrow(() -> new DemandeIntrouvableException(String.valueOf(id)));
    }

    private DemandeCotation chargerParReference(String reference) {
        return repository.trouverParReference(new ReferenceDemande(reference))
                .orElseThrow(() -> new DemandeIntrouvableException(reference));
    }

    private void publierEvenements(DemandeCotation demande) {
        for (DemandeCotationEmiseEvent evenement : demande.evenementsNonPublies()) {
            publication.publier(evenement);
        }
        demande.viderEvenements();
    }
}
