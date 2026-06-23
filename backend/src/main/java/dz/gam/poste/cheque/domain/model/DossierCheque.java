package dz.gam.poste.cheque.domain.model;

import dz.gam.poste.cheque.domain.event.ChequeStatutFinaliseEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dossier de SUIVI d'un chèque. C'est l'agrégat racine du périmètre « Suivi des chèques ».
 *
 * <p>Attention au périmètre (ADR 0003) : ce dossier représente <em>où en est</em> le
 * chèque dans le workflow du poste — pas le règlement lui-même, qui reste la propriété
 * de PROASSUR. On ne stocke ici aucune vérité comptable.
 *
 * <p>L'identité ({@code id}) est propre au poste. Le lien vers l'ERP est la
 * {@link ReferenceCheque}.
 */
public class DossierCheque {

    private final UUID id;
    private final ReferenceCheque reference;
    private final Montant montant;
    private final String beneficiaire;
    private final String agence;
    private final LocalDate dateEmission;

    private StatutCheque statut;
    private final Instant dateCreation;
    private Instant dateDerniereMaj;

    /** Événements de domaine produits et pas encore publiés. Non persisté. */
    private final transient List<ChequeStatutFinaliseEvent> evenements = new ArrayList<>();

    private DossierCheque(UUID id, ReferenceCheque reference, Montant montant, String beneficiaire,
                          String agence, LocalDate dateEmission, StatutCheque statut,
                          Instant dateCreation, Instant dateDerniereMaj) {
        this.id = Objects.requireNonNull(id);
        this.reference = Objects.requireNonNull(reference);
        this.montant = Objects.requireNonNull(montant);
        this.beneficiaire = exigerNonVide(beneficiaire, "bénéficiaire");
        this.agence = exigerNonVide(agence, "agence");
        this.dateEmission = Objects.requireNonNull(dateEmission, "dateEmission obligatoire");
        this.statut = Objects.requireNonNull(statut);
        this.dateCreation = Objects.requireNonNull(dateCreation);
        this.dateDerniereMaj = Objects.requireNonNull(dateDerniereMaj);
    }

    /**
     * Crée un dossier à la réception d'un chèque émis par PROASSUR. Statut initial : ÉMIS.
     */
    public static DossierCheque creerDepuisEmission(ReferenceCheque reference, Montant montant,
                                                    String beneficiaire, String agence,
                                                    LocalDate dateEmission, Instant maintenant) {
        return new DossierCheque(UUID.randomUUID(), reference, montant, beneficiaire, agence,
                dateEmission, StatutCheque.EMIS, maintenant, maintenant);
    }

    /**
     * Reconstitue un dossier depuis l'état persisté (utilisé par l'adapter de persistance).
     * Ne produit aucun événement de domaine.
     */
    public static DossierCheque reconstituer(UUID id, ReferenceCheque reference, Montant montant,
                                             String beneficiaire, String agence, LocalDate dateEmission,
                                             StatutCheque statut, Instant dateCreation, Instant dateDerniereMaj) {
        return new DossierCheque(id, reference, montant, beneficiaire, agence, dateEmission,
                statut, dateCreation, dateDerniereMaj);
    }

    /**
     * Fait avancer le statut selon les règles du cycle de vie.
     * Si le statut cible est terminal, produit un {@link ChequeStatutFinaliseEvent}
     * (boucle fermée).
     *
     * @throws TransitionStatutInvalideException si la transition n'est pas autorisée
     */
    public void faireAvancerVers(StatutCheque cible, Instant maintenant) {
        Objects.requireNonNull(cible, "statut cible obligatoire");
        if (!statut.peutTransitionnerVers(cible)) {
            throw new TransitionStatutInvalideException(statut, cible);
        }
        this.statut = cible;
        this.dateDerniereMaj = maintenant;
        if (cible.estTerminal()) {
            evenements.add(new ChequeStatutFinaliseEvent(reference, cible, maintenant));
        }
    }

    /** Événements produits non encore publiés (le service les draine puis les vide). */
    public List<ChequeStatutFinaliseEvent> evenementsNonPublies() {
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

    public ReferenceCheque reference() {
        return reference;
    }

    public Montant montant() {
        return montant;
    }

    public String beneficiaire() {
        return beneficiaire;
    }

    public String agence() {
        return agence;
    }

    public LocalDate dateEmission() {
        return dateEmission;
    }

    public StatutCheque statut() {
        return statut;
    }

    public Instant dateCreation() {
        return dateCreation;
    }

    public Instant dateDerniereMaj() {
        return dateDerniereMaj;
    }
}
