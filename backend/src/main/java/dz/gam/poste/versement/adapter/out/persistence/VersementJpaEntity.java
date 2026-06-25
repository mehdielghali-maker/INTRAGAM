package dz.gam.poste.versement.adapter.out.persistence;

import dz.gam.poste.versement.domain.model.StatutVersement;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** État persisté d'un versement bancaire. Table {@code versement}. */
@Entity
@Table(name = "versement")
public class VersementJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true)
    private String reference;          // nul tant que brouillon

    @Column(nullable = false)
    private String codeAgence;

    @Column(nullable = false, length = 7)
    private String moisSituation;      // AAAA-MM

    @Column(nullable = false)
    private BigDecimal montantVerse;

    @Column(nullable = false)
    private LocalDate dateVersement;

    private String referenceBordereau;
    private String banque;

    @Column(length = 2000)
    private String commentaire;

    @Column(nullable = false)
    private String createur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutVersement statut;

    private String referenceBpm;
    private String motifRejet;
    private Instant dateDepot;

    @Column(nullable = false)
    private Instant dateCreation;

    @Column(nullable = false)
    private Instant dateMaj;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "versement_piece", joinColumns = @JoinColumn(name = "versement_id"))
    private List<PieceEmbeddable> pieces = new ArrayList<>();

    protected VersementJpaEntity() {
    }

    @Embeddable
    public static class PieceEmbeddable {
        private String nomFichier;
        private String gedId;

        protected PieceEmbeddable() {
        }

        public PieceEmbeddable(String nomFichier, String gedId) {
            this.nomFichier = nomFichier;
            this.gedId = gedId;
        }

        public String getNomFichier() {
            return nomFichier;
        }

        public String getGedId() {
            return gedId;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCodeAgence() {
        return codeAgence;
    }

    public void setCodeAgence(String codeAgence) {
        this.codeAgence = codeAgence;
    }

    public String getMoisSituation() {
        return moisSituation;
    }

    public void setMoisSituation(String moisSituation) {
        this.moisSituation = moisSituation;
    }

    public BigDecimal getMontantVerse() {
        return montantVerse;
    }

    public void setMontantVerse(BigDecimal montantVerse) {
        this.montantVerse = montantVerse;
    }

    public LocalDate getDateVersement() {
        return dateVersement;
    }

    public void setDateVersement(LocalDate dateVersement) {
        this.dateVersement = dateVersement;
    }

    public String getReferenceBordereau() {
        return referenceBordereau;
    }

    public void setReferenceBordereau(String referenceBordereau) {
        this.referenceBordereau = referenceBordereau;
    }

    public String getBanque() {
        return banque;
    }

    public void setBanque(String banque) {
        this.banque = banque;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getCreateur() {
        return createur;
    }

    public void setCreateur(String createur) {
        this.createur = createur;
    }

    public StatutVersement getStatut() {
        return statut;
    }

    public void setStatut(StatutVersement statut) {
        this.statut = statut;
    }

    public String getReferenceBpm() {
        return referenceBpm;
    }

    public void setReferenceBpm(String referenceBpm) {
        this.referenceBpm = referenceBpm;
    }

    public String getMotifRejet() {
        return motifRejet;
    }

    public void setMotifRejet(String motifRejet) {
        this.motifRejet = motifRejet;
    }

    public Instant getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(Instant dateDepot) {
        this.dateDepot = dateDepot;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Instant getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(Instant dateMaj) {
        this.dateMaj = dateMaj;
    }

    public List<PieceEmbeddable> getPieces() {
        return pieces;
    }

    public void setPieces(List<PieceEmbeddable> pieces) {
        this.pieces = pieces;
    }
}
