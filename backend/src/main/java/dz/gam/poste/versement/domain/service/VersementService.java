package dz.gam.poste.versement.domain.service;

import dz.gam.poste.shared.regularisation.EcartRegularisation;
import dz.gam.poste.versement.config.VersementProperties;
import dz.gam.poste.versement.domain.event.VersementBancaireDeposeEvent;
import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.PieceJustificative;
import dz.gam.poste.versement.domain.model.SituationMois;
import dz.gam.poste.versement.domain.model.Versement;
import dz.gam.poste.versement.domain.model.VersementIntrouvableException;
import dz.gam.poste.versement.domain.port.in.ConsulterSituationMoisUseCase;
import dz.gam.poste.versement.domain.port.in.ConsulterVersementsUseCase;
import dz.gam.poste.versement.domain.port.in.DeposerVersementCommand;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.in.RetourBpmUseCase;
import dz.gam.poste.versement.domain.port.in.SoumettreVersementUseCase;
import dz.gam.poste.versement.domain.port.out.GedVersementPort;
import dz.gam.poste.versement.domain.port.out.ProductionEncaissePort;
import dz.gam.poste.versement.domain.port.out.PublicationVersementPort;
import dz.gam.poste.versement.domain.port.out.VersementRepository;
import dz.gam.poste.versement.domain.port.out.VersementsBanquePort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service applicatif de la « Versement bancaire ». Java pur. Il rattache le reçu, lit la
 * situation du mois (PROASSUR/Sage) et orchestre la boucle BPM ; il ne recalcule jamais la
 * comptabilité. Le « reste à régulariser » utilise le calcul partagé avec l'accueil.
 */
public class VersementService implements SoumettreVersementUseCase, ConsulterVersementsUseCase,
        ConsulterSituationMoisUseCase, RetourBpmUseCase {

    private final VersementRepository repository;
    private final PublicationVersementPort publication;
    private final GedVersementPort ged;
    private final ProductionEncaissePort production;
    private final VersementsBanquePort banque;
    private final VersementProperties properties;
    private final Clock horloge;

    public VersementService(VersementRepository repository, PublicationVersementPort publication,
                            GedVersementPort ged, ProductionEncaissePort production, VersementsBanquePort banque,
                            VersementProperties properties, Clock horloge) {
        this.repository = repository;
        this.publication = publication;
        this.ged = ged;
        this.production = production;
        this.banque = banque;
        this.properties = properties;
        this.horloge = horloge;
    }

    @Override
    public SituationMois situation(String codeAgence, MoisSituation mois) {
        ProductionEncaissePort.SituationProduction p = production.productionEtEncaisse(codeAgence, mois);
        var verse = banque.montantDejaVerse(codeAgence, mois);
        VersementProperties.SeuilRegularisation seuil = properties.seuilRegularisation();
        EcartRegularisation.Resultat r = EcartRegularisation.calculer(
                p.encaisse(), verse, seuil.montant(), seuil.pourcentage());
        return new SituationMois(codeAgence, mois.valeur(), mois.libelle(),
                p.productionEmise(), p.encaisse(), verse, r.valeur(), r.pourcentage(), r.aRegulariser());
    }

    @Override
    public Versement enregistrerBrouillon(String codeAgence, String createur, DeposerVersementCommand commande) {
        return repository.enregistrer(construireBrouillon(codeAgence, createur, commande));
    }

    @Override
    public Versement soumettre(String codeAgence, String createur, DeposerVersementCommand commande) {
        Versement versement = construireBrouillon(codeAgence, createur, commande);
        versement.soumettre(genererReference(), horloge.instant());
        Versement enregistre = repository.enregistrer(versement);
        publierEvenements(versement);
        return enregistre;
    }

    @Override
    public Versement soumettreBrouillon(UUID id) {
        Versement versement = charger(id);
        versement.soumettre(genererReference(), horloge.instant());
        Versement enregistre = repository.enregistrer(versement);
        publierEvenements(versement);
        return enregistre;
    }

    @Override
    public List<Versement> lister(FiltreVersement filtre) {
        return repository.lister(filtre);
    }

    @Override
    public Versement obtenir(UUID id) {
        return charger(id);
    }

    @Override
    public long compterEnCours() {
        return repository.compterEnCours();
    }

    @Override
    public void mettreEnControle(String reference, String referenceBpm) {
        Versement versement = chargerParReference(reference);
        versement.mettreEnControle(referenceBpm, horloge.instant());
        repository.enregistrer(versement);
    }

    @Override
    public void valider(String reference) {
        Versement versement = chargerParReference(reference);
        versement.valider(horloge.instant());
        repository.enregistrer(versement);
    }

    @Override
    public void rejeter(String reference, String motif) {
        Versement versement = chargerParReference(reference);
        versement.rejeter(motif, horloge.instant());
        repository.enregistrer(versement);
    }

    private Versement construireBrouillon(String codeAgence, String createur, DeposerVersementCommand c) {
        List<PieceJustificative> pieces = new ArrayList<>();
        if (c.nomsFichiers() != null) {
            for (String nom : c.nomsFichiers()) {
                if (nom != null && !nom.isBlank()) {
                    pieces.add(ged.deposer(nom)); // dépose en GED OneBase, ne garde que la référence
                }
            }
        }
        return Versement.creerBrouillon(codeAgence, MoisSituation.depuis(c.moisSituation()), c.montantVerse(),
                c.dateVersement(), c.referenceBordereau(), c.banque(), c.commentaire(), createur, pieces,
                horloge.instant());
    }

    private String genererReference() {
        long numero = properties.referenceSeed() + repository.compterReferences() + 1;
        int annee = LocalDate.now(horloge).getYear();
        return "%s-%d-%04d".formatted(properties.prefixeReference(), annee, numero);
    }

    private Versement charger(UUID id) {
        return repository.trouverParId(id).orElseThrow(() -> new VersementIntrouvableException(String.valueOf(id)));
    }

    private Versement chargerParReference(String reference) {
        return repository.trouverParReference(reference)
                .orElseThrow(() -> new VersementIntrouvableException(reference));
    }

    private void publierEvenements(Versement versement) {
        for (VersementBancaireDeposeEvent evenement : versement.evenementsNonPublies()) {
            publication.publier(evenement);
        }
        versement.viderEvenements();
    }
}
