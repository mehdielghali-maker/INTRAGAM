package dz.gam.poste.cheque.adapter.out.persistence;

import dz.gam.poste.cheque.domain.model.StatutCheque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Représentation persistée du dossier de suivi. Table {@code dossier_cheque}.
 *
 * <p>Périmètre (ADR 0004) : on ne stocke QUE l'état propre au poste (suivi du chèque).
 * Aucune écriture comptable, aucune copie du transactionnel PROASSUR.
 */
@Entity
@Table(name = "dossier_cheque")
public class DossierChequeJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(nullable = false, length = 3)
    private String devise;

    @Column(nullable = false)
    private String beneficiaire;

    @Column(nullable = false)
    private String agence;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutCheque statut;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_derniere_maj", nullable = false)
    private Instant dateDerniereMaj;

    protected DossierChequeJpaEntity() {
        // requis par JPA
    }

    public DossierChequeJpaEntity(UUID id, String reference, BigDecimal montant, String devise,
                                  String beneficiaire, String agence, LocalDate dateEmission,
                                  StatutCheque statut, Instant dateCreation, Instant dateDerniereMaj) {
        this.id = id;
        this.reference = reference;
        this.montant = montant;
        this.devise = devise;
        this.beneficiaire = beneficiaire;
        this.agence = agence;
        this.dateEmission = dateEmission;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateDerniereMaj = dateDerniereMaj;
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public String getDevise() {
        return devise;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public String getAgence() {
        return agence;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public StatutCheque getStatut() {
        return statut;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public Instant getDateDerniereMaj() {
        return dateDerniereMaj;
    }
}
