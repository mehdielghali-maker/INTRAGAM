package dz.gam.poste.versement.domain.model;

import dz.gam.poste.versement.domain.event.VersementBancaireDeposeEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat racine du périmètre « Versement bancaire » : le dépôt d'un reçu justifiant
 * l'encaissement des primes émises sur un mois, pour une agence. Le poste rattache et suit
 * (workflow BPM) ; il ne recalcule pas la comptabilité (ADR 0003).
 */
public class Versement {

    private final UUID id;
    private String reference;                 // null tant que brouillon
    private final String codeAgence;
    private final MoisSituation mois;
    private final BigDecimal montantVerse;
    private final LocalDate dateVersement;
    private final String referenceBordereau;  // facultatif
    private final String banque;              // facultatif
    private final String commentaire;         // facultatif
    private final String createur;
    private final List<PieceJustificative> pieces;

    private StatutVersement statut;
    private String referenceBpm;
    private String motifRejet;
    private Instant dateDepot;
    private final Instant dateCreation;
    private Instant dateMaj;

    private final transient List<VersementBancaireDeposeEvent> evenements = new ArrayList<>();

    private Versement(UUID id, String reference, String codeAgence, MoisSituation mois, BigDecimal montantVerse,
                      LocalDate dateVersement, String referenceBordereau, String banque, String commentaire,
                      String createur, List<PieceJustificative> pieces, StatutVersement statut, String referenceBpm,
                      String motifRejet, Instant dateDepot, Instant dateCreation, Instant dateMaj) {
        this.id = Objects.requireNonNull(id);
        this.reference = reference;
        this.codeAgence = exigerNonVide(codeAgence, "agence");
        this.mois = Objects.requireNonNull(mois, "mois obligatoire");
        this.montantVerse = Objects.requireNonNull(montantVerse, "montant obligatoire");
        this.dateVersement = Objects.requireNonNull(dateVersement, "date de versement obligatoire");
        this.referenceBordereau = referenceBordereau;
        this.banque = banque;
        this.commentaire = commentaire;
        this.createur = exigerNonVide(createur, "créateur");
        this.pieces = new ArrayList<>(pieces == null ? List.of() : pieces);
        this.statut = Objects.requireNonNull(statut);
        this.referenceBpm = referenceBpm;
        this.motifRejet = motifRejet;
        this.dateDepot = dateDepot;
        this.dateCreation = Objects.requireNonNull(dateCreation);
        this.dateMaj = Objects.requireNonNull(dateMaj);
    }

    /** Crée un versement en BROUILLON (montant éventuellement nul tant qu'il n'est pas soumis). */
    public static Versement creerBrouillon(String codeAgence, MoisSituation mois, BigDecimal montantVerse,
                                           LocalDate dateVersement, String referenceBordereau, String banque,
                                           String commentaire, String createur, List<PieceJustificative> pieces,
                                           Instant maintenant) {
        BigDecimal montant = montantVerse == null ? BigDecimal.ZERO : montantVerse;
        if (montant.signum() < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif");
        }
        return new Versement(UUID.randomUUID(), null, codeAgence, mois, montant, dateVersement,
                referenceBordereau, banque, commentaire, createur, pieces, StatutVersement.BROUILLON,
                null, null, null, maintenant, maintenant);
    }

    /** Reconstitue depuis l'état persisté (aucun événement produit). */
    public static Versement reconstituer(UUID id, String reference, String codeAgence, MoisSituation mois,
                                         BigDecimal montantVerse, LocalDate dateVersement, String referenceBordereau,
                                         String banque, String commentaire, String createur,
                                         List<PieceJustificative> pieces, StatutVersement statut, String referenceBpm,
                                         String motifRejet, Instant dateDepot, Instant dateCreation, Instant dateMaj) {
        return new Versement(id, reference, codeAgence, mois, montantVerse, dateVersement, referenceBordereau,
                banque, commentaire, createur, pieces, statut, referenceBpm, motifRejet, dateDepot,
                dateCreation, dateMaj);
    }

    /**
     * Soumet le versement au BPM. Exige une pièce (reçu obligatoire) et un montant
     * strictement positif. Affecte la référence, passe à DÉPOSÉ et produit l'événement.
     */
    public void soumettre(String reference, Instant maintenant) {
        if (!statut.peutTransitionnerVers(StatutVersement.DEPOSE)) {
            throw new TransitionVersementInvalideException(statut, StatutVersement.DEPOSE);
        }
        if (pieces.isEmpty()) {
            throw new IllegalArgumentException("Le reçu de versement est obligatoire");
        }
        if (montantVerse.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
        this.reference = Objects.requireNonNull(reference, "référence obligatoire");
        this.statut = StatutVersement.DEPOSE;
        this.dateDepot = maintenant;
        this.dateMaj = maintenant;
        evenements.add(new VersementBancaireDeposeEvent(reference, codeAgence, mois.valeur(),
                montantVerse, dateVersement, maintenant));
    }

    /** Retour BPM : prise en contrôle. */
    public void mettreEnControle(String referenceBpm, Instant maintenant) {
        transitionner(StatutVersement.EN_CONTROLE, maintenant);
        if (referenceBpm != null && !referenceBpm.isBlank()) {
            this.referenceBpm = referenceBpm;
        }
    }

    /** Retour BPM : versement validé (terminal). */
    public void valider(Instant maintenant) {
        transitionner(StatutVersement.VALIDE, maintenant);
    }

    /** Retour BPM : versement rejeté (terminal) avec motif. */
    public void rejeter(String motif, Instant maintenant) {
        transitionner(StatutVersement.REJETE, maintenant);
        this.motifRejet = motif;
    }

    private void transitionner(StatutVersement cible, Instant maintenant) {
        if (!statut.peutTransitionnerVers(cible)) {
            throw new TransitionVersementInvalideException(statut, cible);
        }
        this.statut = cible;
        this.dateMaj = maintenant;
    }

    public List<VersementBancaireDeposeEvent> evenementsNonPublies() {
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

    public String reference() {
        return reference;
    }

    public String codeAgence() {
        return codeAgence;
    }

    public MoisSituation mois() {
        return mois;
    }

    public BigDecimal montantVerse() {
        return montantVerse;
    }

    public LocalDate dateVersement() {
        return dateVersement;
    }

    public String referenceBordereau() {
        return referenceBordereau;
    }

    public String banque() {
        return banque;
    }

    public String commentaire() {
        return commentaire;
    }

    public String createur() {
        return createur;
    }

    public List<PieceJustificative> pieces() {
        return Collections.unmodifiableList(pieces);
    }

    public StatutVersement statut() {
        return statut;
    }

    public String referenceBpm() {
        return referenceBpm;
    }

    public String motifRejet() {
        return motifRejet;
    }

    public Instant dateDepot() {
        return dateDepot;
    }

    public Instant dateCreation() {
        return dateCreation;
    }

    public Instant dateMaj() {
        return dateMaj;
    }
}
