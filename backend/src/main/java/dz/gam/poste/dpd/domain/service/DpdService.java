package dz.gam.poste.dpd.domain.service;

import dz.gam.poste.dpd.config.DpdProperties;
import dz.gam.poste.dpd.domain.event.DemandePaiementDiffereEmiseEvent;
import dz.gam.poste.dpd.domain.event.EcheancierDpdMisAJourEvent;
import dz.gam.poste.dpd.domain.model.AccordSuivi;
import dz.gam.poste.dpd.domain.model.ContexteDpd;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.DpdExceptions;
import dz.gam.poste.dpd.domain.model.IdentiteDpd;
import dz.gam.poste.dpd.domain.model.InfoClient;
import dz.gam.poste.dpd.domain.model.PieceJointeDpd;
import dz.gam.poste.dpd.domain.model.ReferenceDpd;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import dz.gam.poste.dpd.domain.model.TypePiece;
import dz.gam.poste.dpd.domain.model.Validateur;
import dz.gam.poste.dpd.domain.port.in.ConsulterDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.CreerDpdCommand;
import dz.gam.poste.dpd.domain.port.in.EnregistrerDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.in.MiseAJourDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.RetourValidationUseCase;
import dz.gam.poste.dpd.domain.port.out.AccordProassur;
import dz.gam.poste.dpd.domain.port.out.AccordSuiviRepository;
import dz.gam.poste.dpd.domain.port.out.BpmDpdPort;
import dz.gam.poste.dpd.domain.port.out.DemandeDpdRepository;
import dz.gam.poste.dpd.domain.port.out.GedDpdPort;
import dz.gam.poste.dpd.domain.port.out.IdentiteDpdPort;
import dz.gam.poste.dpd.domain.port.out.PrefillProposition;
import dz.gam.poste.dpd.domain.port.out.ProassurDpdPort;
import dz.gam.poste.dpd.domain.port.out.PublicationDpdPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service applicatif des accords d'échéancier (DPD). Java pur. Capte la demande
 * pré-remplie depuis PROASSUR, la route pour validation, suit son statut et reflète
 * l'échéancier validé. Ne saisit ni ne recalcule l'échéancier ou l'état de règlement.
 */
public class DpdService implements EnregistrerDpdUseCase, RetourValidationUseCase,
        ConsulterDpdUseCase, MiseAJourDpdUseCase {

    private final DemandeDpdRepository demandes;
    private final AccordSuiviRepository accords;
    private final ProassurDpdPort proassur;
    private final BpmDpdPort bpm;
    private final GedDpdPort ged;
    private final IdentiteDpdPort identite;
    private final PublicationDpdPort publication;
    private final DpdProperties properties;
    private final Clock horloge;

    public DpdService(DemandeDpdRepository demandes, AccordSuiviRepository accords, ProassurDpdPort proassur,
                      BpmDpdPort bpm, GedDpdPort ged, IdentiteDpdPort identite, PublicationDpdPort publication,
                      DpdProperties properties, Clock horloge) {
        this.demandes = demandes;
        this.accords = accords;
        this.proassur = proassur;
        this.bpm = bpm;
        this.ged = ged;
        this.identite = identite;
        this.publication = publication;
        this.properties = properties;
        this.horloge = horloge;
    }

    @Override
    public DemandeDpd enregistrerBrouillon(CreerDpdCommand commande) {
        return demandes.enregistrer(construireBrouillon(commande));
    }

    @Override
    public DemandeDpd envoyer(CreerDpdCommand commande) {
        DemandeDpd demande = construireBrouillon(commande);
        demande.envoyer(genererReference(), horloge.instant());
        DemandeDpd enregistree = demandes.enregistrer(demande);
        publierDemande(demande);
        return enregistree;
    }

    @Override
    public DemandeDpd envoyerBrouillon(UUID idDemande) {
        DemandeDpd demande = chargerDemande(idDemande);
        demande.envoyer(genererReference(), horloge.instant());
        DemandeDpd enregistree = demandes.enregistrer(demande);
        publierDemande(demande);
        return enregistree;
    }

    @Override
    public void mettreEnValidation(String reference) {
        DemandeDpd demande = chargerParReference(reference);
        // Source BPM : nom du validateur DR/central qui prend en charge.
        Validateur validateur = bpm.getValidateur(reference)
                .orElseThrow(() -> new IllegalStateException("Validateur BPM absent pour " + reference));
        demande.mettreEnValidation(validateur, horloge.instant());
        demandes.enregistrer(demande);
    }

    @Override
    public void accorder(String reference, String codeAccord) {
        DemandeDpd demande = chargerParReference(reference);
        demande.accorder(codeAccord, horloge.instant());
        demandes.enregistrer(demande);
        // Source PROASSUR : échéancier validé, historisé en version 1 du suivi de l'accord.
        AccordProassur accord = proassur.getAccordByCode(codeAccord)
                .orElseThrow(() -> new DpdExceptions.AccordIntrouvable(codeAccord));
        accords.enregistrer(AccordSuivi.creer(codeAccord, accord.resume(), accord.echeances(), horloge.instant()));
    }

    @Override
    public void refuser(String reference, String motif) {
        DemandeDpd demande = chargerParReference(reference);
        demande.refuser(motif, horloge.instant());
        demandes.enregistrer(demande);
    }

    @Override
    public List<DemandeDpd> lister(FiltreDpd filtre) {
        return demandes.lister(filtre);
    }

    @Override
    public DemandeDpd obtenir(UUID idDemande) {
        return chargerDemande(idDemande);
    }

    @Override
    public long compterActives() {
        return demandes.lister(FiltreDpd.aucun()).stream().filter(d -> d.statut().estActive()).count();
    }

    @Override
    public ContexteDpd contexte() {
        Map<String, String> libelles = new LinkedHashMap<>();
        for (StatutDpd s : StatutDpd.values()) {
            libelles.put(s.name(), properties.libelles().getOrDefault(s, s.name()));
        }
        return new ContexteDpd(identiteCourante(), libelles, properties.niveauValidation());
    }

    @Override
    public Optional<PrefillProposition> prefill(String noProposition) {
        return proassur.prefillFromProposition(noProposition);
    }

    @Override
    public Optional<AccordProassur> chargerAccord(String codeAccord) {
        return proassur.getAccordByCode(codeAccord);
    }

    @Override
    public AccordSuivi synchroniser(String codeAccord) {
        AccordProassur accord = proassur.getAccordByCode(codeAccord)
                .orElseThrow(() -> new DpdExceptions.AccordIntrouvable(codeAccord));
        Optional<AccordSuivi> existant = accords.trouverParCodeAccord(codeAccord);

        AccordSuivi suivi;
        if (existant.isPresent()) {
            suivi = existant.get();
            suivi.mettreAJour(accord.resume(), accord.echeances(), horloge.instant());
            publierAccord(suivi);
        } else {
            // Premier suivi créé via l'action « Mettre à jour » : on émet l'événement pour la v1.
            suivi = AccordSuivi.creer(codeAccord, accord.resume(), accord.echeances(), horloge.instant());
            publication.publier(new EcheancierDpdMisAJourEvent(codeAccord, 1, horloge.instant()));
        }
        return accords.enregistrer(suivi);
    }

    private DemandeDpd construireBrouillon(CreerDpdCommand cmd) {
        PrefillProposition prefill = proassur.prefillFromProposition(cmd.noProposition())
                .orElseThrow(() -> new DpdExceptions.AccordIntrouvable("proposition " + cmd.noProposition()));
        IdentiteDpd id = identiteCourante();
        InfoClient client = new InfoClient(cmd.nomAssure(), cmd.nomSouscripteur(), cmd.telephone(), cmd.cnrc(),
                cmd.typePersonne(), cmd.institutionPublique(), cmd.adresse());

        List<PieceJointeDpd> pieces = new ArrayList<>();
        deposer(pieces, TypePiece.RC, cmd.fichiersRc());
        deposer(pieces, TypePiece.AUTRE, cmd.fichiersAutres());

        return DemandeDpd.creerBrouillon(prefill.souscription(), client, cmd.avenant(), cmd.commentaire(),
                id.codeAgence(), id.mailAgence(), id.directionRegionale(), id.mailDirectionRegionale(),
                id.utilisateur(), pieces, horloge.instant());
    }

    private void deposer(List<PieceJointeDpd> cible, TypePiece type, List<String> noms) {
        if (noms == null) {
            return;
        }
        for (String nom : noms) {
            if (nom != null && !nom.isBlank()) {
                cible.add(ged.deposer(type, nom)); // dépose en GED OneBase, ne garde que la référence
            }
        }
    }

    private IdentiteDpd identiteCourante() {
        return identite.identiteCourante();
    }

    private ReferenceDpd genererReference() {
        long numero = properties.referenceSeed() + demandes.compterReferencees() + 1;
        int annee = LocalDate.now(horloge).getYear();
        return new ReferenceDpd("%s-%d-%04d".formatted(properties.prefixeReference(), annee, numero));
    }

    private DemandeDpd chargerDemande(UUID id) {
        return demandes.trouverParId(id).orElseThrow(() -> new DpdExceptions.DemandeIntrouvable(String.valueOf(id)));
    }

    private DemandeDpd chargerParReference(String reference) {
        return demandes.trouverParReference(new ReferenceDpd(reference))
                .orElseThrow(() -> new DpdExceptions.DemandeIntrouvable(reference));
    }

    private void publierDemande(DemandeDpd demande) {
        for (DemandePaiementDiffereEmiseEvent e : demande.evenementsNonPublies()) {
            publication.publier(e);
        }
        demande.viderEvenements();
    }

    private void publierAccord(AccordSuivi accord) {
        for (EcheancierDpdMisAJourEvent e : accord.evenementsNonPublies()) {
            publication.publier(e);
        }
        accord.viderEvenements();
    }
}
