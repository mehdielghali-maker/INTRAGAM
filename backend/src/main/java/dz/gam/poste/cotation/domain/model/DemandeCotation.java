package dz.gam.poste.cotation.domain.model;

import dz.gam.poste.cotation.domain.event.DemandeCotationEmiseEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Demande de cotation suivie par le poste. Agrégat racine.
 *
 * <p>Le poste ORCHESTRE : il transporte la demande vers le central et suit son statut.
 * Il ne tarifie jamais et ne réplique pas le contenu du devis. Les champs renvoyés par
 * le central (souscripteur via BPM ; n° proposition / réf devis / n° police via PROASSUR)
 * sont seulement mémorisés pour affichage.
 */
public class DemandeCotation {

    private final UUID id;
    private ReferenceDemande reference;          // poste — générée à l'envoi
    private final String objet;                  // poste — branche/objet saisi
    private final String nomProspect;            // poste
    private String numeroPolice;                 // poste (renouvellement) puis PROASSUR (affaire gagnée)
    private final boolean renouvellement;        // poste — nature à la création
    private final String commentaire;            // poste
    private final String codeAgence;             // identité SSO
    private final String directionRegionale;     // identité SSO
    private final String createur;               // identité SSO (utilisateur connecté)
    private final List<PieceJointe> piecesJointes; // références GED OneBase

    private StatutCotation statut;
    private Souscripteur souscripteur;           // BPM/OneBase — dès « En cours »
    private String numeroProposition;            // PROASSUR — à « À finaliser »
    private String referenceDevis;               // PROASSUR — à « À finaliser »
    private String motifSansSuite;               // poste — à « Sans suite »
    private Instant dateQuittance;               // PROASSUR — à « Affaire gagnée »

    private final Instant dateCreation;
    private Instant dateMaj;

    private final transient List<DemandeCotationEmiseEvent> evenements = new ArrayList<>();

    private DemandeCotation(UUID id, ReferenceDemande reference, String objet, String nomProspect,
                            String numeroPolice, boolean renouvellement, String commentaire, String codeAgence,
                            String directionRegionale, String createur, List<PieceJointe> piecesJointes,
                            StatutCotation statut, Souscripteur souscripteur, String numeroProposition,
                            String referenceDevis, String motifSansSuite, Instant dateQuittance,
                            Instant dateCreation, Instant dateMaj) {
        this.id = Objects.requireNonNull(id);
        this.reference = reference;
        this.objet = exigerNonVide(objet, "objet");
        this.nomProspect = exigerNonVide(nomProspect, "nom du prospect");
        this.numeroPolice = videEnNull(numeroPolice);
        this.renouvellement = renouvellement;
        this.commentaire = commentaire == null ? "" : commentaire.trim();
        this.codeAgence = exigerNonVide(codeAgence, "code agence");
        this.directionRegionale = exigerNonVide(directionRegionale, "direction régionale");
        this.createur = exigerNonVide(createur, "créateur");
        this.piecesJointes = new ArrayList<>(piecesJointes == null ? List.of() : piecesJointes);
        this.statut = Objects.requireNonNull(statut);
        this.souscripteur = souscripteur;
        this.numeroProposition = numeroProposition;
        this.referenceDevis = referenceDevis;
        this.motifSansSuite = motifSansSuite;
        this.dateQuittance = dateQuittance;
        this.dateCreation = Objects.requireNonNull(dateCreation);
        this.dateMaj = Objects.requireNonNull(dateMaj);
    }

    /** Crée une demande en BROUILLON (pas encore de N° demande). */
    public static DemandeCotation creerBrouillon(String objet, String nomProspect, String numeroPolice,
                                                 String commentaire, String codeAgence, String directionRegionale,
                                                 String createur, List<PieceJointe> piecesJointes, Instant maintenant) {
        boolean renouvellement = videEnNull(numeroPolice) != null;
        return new DemandeCotation(UUID.randomUUID(), null, objet, nomProspect, numeroPolice, renouvellement,
                commentaire, codeAgence, directionRegionale, createur, piecesJointes,
                StatutCotation.BROUILLON, null, null, null, null, null, maintenant, maintenant);
    }

    /** Reconstitue depuis l'état persisté (aucun événement produit). */
    public static DemandeCotation reconstituer(UUID id, ReferenceDemande reference, String objet, String nomProspect,
                                               String numeroPolice, boolean renouvellement, String commentaire,
                                               String codeAgence, String directionRegionale, String createur,
                                               List<PieceJointe> piecesJointes, StatutCotation statut,
                                               Souscripteur souscripteur, String numeroProposition, String referenceDevis,
                                               String motifSansSuite, Instant dateQuittance, Instant dateCreation,
                                               Instant dateMaj) {
        return new DemandeCotation(id, reference, objet, nomProspect, numeroPolice, renouvellement, commentaire,
                codeAgence, directionRegionale, createur, piecesJointes, statut, souscripteur, numeroProposition,
                referenceDevis, motifSansSuite, dateQuittance, dateCreation, dateMaj);
    }

    /** Action agence : transmet la demande au central. Produit l'événement d'émission. */
    public void envoyer(ReferenceDemande referenceGeneree, Instant maintenant) {
        exigerTransition(StatutCotation.ENVOYEE);
        this.reference = Objects.requireNonNull(referenceGeneree, "référence générée obligatoire");
        this.statut = StatutCotation.ENVOYEE;
        this.dateMaj = maintenant;
        evenements.add(new DemandeCotationEmiseEvent(reference, objet, nomProspect, numeroPolice,
                commentaire, codeAgence, directionRegionale, maintenant));
    }

    /** Retour BPM/OneBase : prise en charge par un souscripteur. */
    public void prendreEnCharge(Souscripteur souscripteurCentral, Instant maintenant) {
        exigerTransition(StatutCotation.EN_COURS);
        this.souscripteur = Objects.requireNonNull(souscripteurCentral, "souscripteur obligatoire");
        this.statut = StatutCotation.EN_COURS;
        this.dateMaj = maintenant;
    }

    /** Retour PROASSUR : cotation faite, devis disponible (référence seulement). */
    public void finaliser(String numeroProposition, String referenceDevis, Instant maintenant) {
        exigerTransition(StatutCotation.A_FINALISER);
        this.numeroProposition = exigerNonVide(numeroProposition, "n° proposition");
        this.referenceDevis = exigerNonVide(referenceDevis, "référence du devis");
        this.statut = StatutCotation.A_FINALISER;
        this.dateMaj = maintenant;
    }

    /** Retour PROASSUR : affaire gagnée à l'impression de la quittance. */
    public void marquerAffaireGagnee(String numeroPolice, Instant maintenant) {
        exigerTransition(StatutCotation.AFFAIRE_GAGNEE);
        this.numeroPolice = exigerNonVide(numeroPolice, "n° police");
        this.dateQuittance = maintenant;
        this.statut = StatutCotation.AFFAIRE_GAGNEE;
        this.dateMaj = maintenant;
    }

    /** Clôture négative : devis non retenu par le client. */
    public void marquerSansSuite(String motif, Instant maintenant) {
        exigerTransition(StatutCotation.SANS_SUITE);
        this.motifSansSuite = motif == null || motif.isBlank() ? "Devis non retenu par le client" : motif.trim();
        this.statut = StatutCotation.SANS_SUITE;
        this.dateMaj = maintenant;
    }

    private void exigerTransition(StatutCotation cible) {
        if (!statut.peutTransitionnerVers(cible)) {
            throw new TransitionCotationInvalideException(statut, cible);
        }
    }

    public List<DemandeCotationEmiseEvent> evenementsNonPublies() {
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

    private static String videEnNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    public UUID id() {
        return id;
    }

    public ReferenceDemande reference() {
        return reference;
    }

    public String objet() {
        return objet;
    }

    public String nomProspect() {
        return nomProspect;
    }

    public String numeroPolice() {
        return numeroPolice;
    }

    public boolean renouvellement() {
        return renouvellement;
    }

    public String commentaire() {
        return commentaire;
    }

    public String codeAgence() {
        return codeAgence;
    }

    public String directionRegionale() {
        return directionRegionale;
    }

    public String createur() {
        return createur;
    }

    public List<PieceJointe> piecesJointes() {
        return Collections.unmodifiableList(piecesJointes);
    }

    public StatutCotation statut() {
        return statut;
    }

    public Souscripteur souscripteur() {
        return souscripteur;
    }

    public String numeroProposition() {
        return numeroProposition;
    }

    public String referenceDevis() {
        return referenceDevis;
    }

    public String motifSansSuite() {
        return motifSansSuite;
    }

    public Instant dateQuittance() {
        return dateQuittance;
    }

    public Instant dateCreation() {
        return dateCreation;
    }

    public Instant dateMaj() {
        return dateMaj;
    }
}
