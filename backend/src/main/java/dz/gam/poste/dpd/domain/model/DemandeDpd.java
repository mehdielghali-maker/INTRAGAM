package dz.gam.poste.dpd.domain.model;

import dz.gam.poste.dpd.domain.event.DemandePaiementDiffereEmiseEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Demande de paiement différé (DPD) suivie par le poste. Agrégat racine.
 *
 * <p>Le poste ORCHESTRE et REFLÈTE : il capte la demande pré-remplie depuis PROASSUR, la
 * transmet pour validation et suit son statut. Il ne saisit ni ne recalcule l'échéancier
 * (propriété de PROASSUR), géré séparément en versions ({@link AccordSuivi}).
 */
public class DemandeDpd {

    private final UUID id;
    private ReferenceDpd reference;              // poste — générée à l'envoi
    private StatutDpd statut;

    private final Souscription souscription;     // PROASSUR (lecture seule)
    private InfoClient infoClient;               // PROASSUR (pré-rempli), éditable
    private final boolean avenant;
    private final String commentaire;

    private final String codeAgence;             // SSO
    private final String mailAgence;             // SSO
    private final String directionRegionale;     // SSO
    private final String mailDirectionRegionale; // SSO
    private final String createur;               // SSO

    private final List<PieceJointeDpd> pieces;   // GED OneBase (références)

    private Validateur validateur;               // BPM — dès « En validation »
    private String motifRefus;                   // central — à « Refusée »
    private String codeAccord;                   // PROASSUR — à « Accordée »

    private final Instant dateDemande;
    private Instant dateMaj;

    private final transient List<DemandePaiementDiffereEmiseEvent> evenements = new ArrayList<>();

    private DemandeDpd(UUID id, ReferenceDpd reference, StatutDpd statut, Souscription souscription,
                       InfoClient infoClient, boolean avenant, String commentaire, String codeAgence,
                       String mailAgence, String directionRegionale, String mailDirectionRegionale,
                       String createur, List<PieceJointeDpd> pieces, Validateur validateur, String motifRefus,
                       String codeAccord, Instant dateDemande, Instant dateMaj) {
        this.id = Objects.requireNonNull(id);
        this.reference = reference;
        this.statut = Objects.requireNonNull(statut);
        this.souscription = Objects.requireNonNull(souscription, "souscription obligatoire (charger la proposition)");
        this.infoClient = Objects.requireNonNull(infoClient, "infoClient obligatoire");
        exigerNonVide(infoClient.nomAssure(), "nom assuré");
        exigerNonVide(infoClient.nomSouscripteur(), "nom souscripteur");
        this.avenant = avenant;
        this.commentaire = commentaire == null ? "" : commentaire.trim();
        this.codeAgence = codeAgence;
        this.mailAgence = mailAgence;
        this.directionRegionale = directionRegionale;
        this.mailDirectionRegionale = mailDirectionRegionale;
        this.createur = createur;
        this.pieces = new ArrayList<>(pieces == null ? List.of() : pieces);
        this.validateur = validateur;
        this.motifRefus = motifRefus;
        this.codeAccord = codeAccord;
        this.dateDemande = Objects.requireNonNull(dateDemande);
        this.dateMaj = Objects.requireNonNull(dateMaj);
    }

    public static DemandeDpd creerBrouillon(Souscription souscription, InfoClient infoClient, boolean avenant,
                                            String commentaire, String codeAgence, String mailAgence,
                                            String directionRegionale, String mailDirectionRegionale, String createur,
                                            List<PieceJointeDpd> pieces, Instant maintenant) {
        return new DemandeDpd(UUID.randomUUID(), null, StatutDpd.BROUILLON, souscription, infoClient, avenant,
                commentaire, codeAgence, mailAgence, directionRegionale, mailDirectionRegionale, createur,
                pieces, null, null, null, maintenant, maintenant);
    }

    public static DemandeDpd reconstituer(UUID id, ReferenceDpd reference, StatutDpd statut, Souscription souscription,
                                          InfoClient infoClient, boolean avenant, String commentaire, String codeAgence,
                                          String mailAgence, String directionRegionale, String mailDirectionRegionale,
                                          String createur, List<PieceJointeDpd> pieces, Validateur validateur,
                                          String motifRefus, String codeAccord, Instant dateDemande, Instant dateMaj) {
        return new DemandeDpd(id, reference, statut, souscription, infoClient, avenant, commentaire, codeAgence,
                mailAgence, directionRegionale, mailDirectionRegionale, createur, pieces, validateur, motifRefus,
                codeAccord, dateDemande, dateMaj);
    }

    /** Transmet la demande pour validation. Exige au moins une pièce de type RC. */
    public void envoyer(ReferenceDpd referenceGeneree, Instant maintenant) {
        exigerTransition(StatutDpd.ENVOYEE);
        if (!aRegistreDeCommerce()) {
            throw new DpdExceptions.RcManquant();
        }
        this.reference = Objects.requireNonNull(referenceGeneree);
        this.statut = StatutDpd.ENVOYEE;
        this.dateMaj = maintenant;
        evenements.add(new DemandePaiementDiffereEmiseEvent(reference, souscription.noProposition(),
                infoClient.nomAssure(), souscription.montantPrime(), souscription.dureeContratMois(), maintenant));
    }

    /** Retour BPM : prise en charge par un validateur DR/central. */
    public void mettreEnValidation(Validateur v, Instant maintenant) {
        exigerTransition(StatutDpd.EN_VALIDATION);
        this.validateur = Objects.requireNonNull(v);
        this.statut = StatutDpd.EN_VALIDATION;
        this.dateMaj = maintenant;
    }

    /** Retour PROASSUR (EcheancierValide) : accord validé, échéancier disponible. */
    public void accorder(String codeAccord, Instant maintenant) {
        exigerTransition(StatutDpd.ACCORDEE);
        this.codeAccord = exigerNonVide(codeAccord, "code accord");
        this.statut = StatutDpd.ACCORDEE;
        this.dateMaj = maintenant;
    }

    /** Refus du central, avec motif. */
    public void refuser(String motif, Instant maintenant) {
        exigerTransition(StatutDpd.REFUSEE);
        this.motifRefus = exigerNonVide(motif, "motif de refus");
        this.statut = StatutDpd.REFUSEE;
        this.dateMaj = maintenant;
    }

    public boolean aRegistreDeCommerce() {
        return pieces.stream().anyMatch(p -> p.type() == TypePiece.RC);
    }

    private void exigerTransition(StatutDpd cible) {
        if (!statut.peutTransitionnerVers(cible)) {
            throw new DpdExceptions.TransitionInvalide(statut, cible);
        }
    }

    public List<DemandePaiementDiffereEmiseEvent> evenementsNonPublies() {
        return Collections.unmodifiableList(evenements);
    }

    public void viderEvenements() {
        evenements.clear();
    }

    private static String exigerNonVide(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ " + champ + " est obligatoire");
        }
        return valeur.trim();
    }

    public UUID id() {
        return id;
    }

    public ReferenceDpd reference() {
        return reference;
    }

    public StatutDpd statut() {
        return statut;
    }

    public Souscription souscription() {
        return souscription;
    }

    public InfoClient infoClient() {
        return infoClient;
    }

    public boolean avenant() {
        return avenant;
    }

    public String commentaire() {
        return commentaire;
    }

    public String codeAgence() {
        return codeAgence;
    }

    public String mailAgence() {
        return mailAgence;
    }

    public String directionRegionale() {
        return directionRegionale;
    }

    public String mailDirectionRegionale() {
        return mailDirectionRegionale;
    }

    public String createur() {
        return createur;
    }

    public List<PieceJointeDpd> pieces() {
        return Collections.unmodifiableList(pieces);
    }

    public Validateur validateur() {
        return validateur;
    }

    public String motifRefus() {
        return motifRefus;
    }

    public String codeAccord() {
        return codeAccord;
    }

    public Instant dateDemande() {
        return dateDemande;
    }

    public Instant dateMaj() {
        return dateMaj;
    }
}
