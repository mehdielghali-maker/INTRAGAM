package dz.gam.poste.cotation.adapter.out.persistence;

import dz.gam.poste.cotation.domain.model.StatutCotation;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** État persisté d'une demande de cotation. Table {@code demande_cotation}. */
@Entity
@Table(name = "demande_cotation")
public class DemandeCotationJpaEntity {

    @Id
    private UUID id;

    @Column(unique = true)
    private String reference;          // nul tant que brouillon

    @Column(nullable = false)
    private String objet;

    @Column(nullable = false)
    private String nomProspect;

    private String numeroPolice;

    @Column(nullable = false)
    private boolean renouvellement;

    @Column(length = 2000)
    private String commentaire;

    @Column(nullable = false)
    private String codeAgence;

    @Column(nullable = false)
    private String directionRegionale;

    @Column(nullable = false)
    private String createur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutCotation statut;

    private String souscripteurNom;
    private String numeroProposition;
    private String referenceDevis;
    private String motifSansSuite;
    private Instant dateQuittance;

    @Column(nullable = false)
    private Instant dateCreation;

    @Column(nullable = false)
    private Instant dateMaj;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "demande_cotation_piece", joinColumns = @JoinColumn(name = "demande_id"))
    private List<PieceJointeEmbeddable> piecesJointes = new ArrayList<>();

    protected DemandeCotationJpaEntity() {
    }

    @Embeddable
    public static class PieceJointeEmbeddable {
        private String nomFichier;
        private String gedId;

        protected PieceJointeEmbeddable() {
        }

        public PieceJointeEmbeddable(String nomFichier, String gedId) {
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

    // Getters / setters (utilisés par l'adapter de persistance pour le mapping)
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

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getNomProspect() {
        return nomProspect;
    }

    public void setNomProspect(String nomProspect) {
        this.nomProspect = nomProspect;
    }

    public String getNumeroPolice() {
        return numeroPolice;
    }

    public void setNumeroPolice(String numeroPolice) {
        this.numeroPolice = numeroPolice;
    }

    public boolean isRenouvellement() {
        return renouvellement;
    }

    public void setRenouvellement(boolean renouvellement) {
        this.renouvellement = renouvellement;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getCodeAgence() {
        return codeAgence;
    }

    public void setCodeAgence(String codeAgence) {
        this.codeAgence = codeAgence;
    }

    public String getDirectionRegionale() {
        return directionRegionale;
    }

    public void setDirectionRegionale(String directionRegionale) {
        this.directionRegionale = directionRegionale;
    }

    public String getCreateur() {
        return createur;
    }

    public void setCreateur(String createur) {
        this.createur = createur;
    }

    public StatutCotation getStatut() {
        return statut;
    }

    public void setStatut(StatutCotation statut) {
        this.statut = statut;
    }

    public String getSouscripteurNom() {
        return souscripteurNom;
    }

    public void setSouscripteurNom(String souscripteurNom) {
        this.souscripteurNom = souscripteurNom;
    }

    public String getNumeroProposition() {
        return numeroProposition;
    }

    public void setNumeroProposition(String numeroProposition) {
        this.numeroProposition = numeroProposition;
    }

    public String getReferenceDevis() {
        return referenceDevis;
    }

    public void setReferenceDevis(String referenceDevis) {
        this.referenceDevis = referenceDevis;
    }

    public String getMotifSansSuite() {
        return motifSansSuite;
    }

    public void setMotifSansSuite(String motifSansSuite) {
        this.motifSansSuite = motifSansSuite;
    }

    public Instant getDateQuittance() {
        return dateQuittance;
    }

    public void setDateQuittance(Instant dateQuittance) {
        this.dateQuittance = dateQuittance;
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

    public List<PieceJointeEmbeddable> getPiecesJointes() {
        return piecesJointes;
    }

    public void setPiecesJointes(List<PieceJointeEmbeddable> piecesJointes) {
        this.piecesJointes = piecesJointes;
    }
}
