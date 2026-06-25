package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** Profil SSO mocké, persisté (AGA/agent + son périmètre d'agences). Géré via l'admin. */
@Entity
@Table(name = "contexte_profil")
public class ProfilAgaEntity {

    @Id
    String identifiant;

    @Column(nullable = false)
    String nomAffiche;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    ProfilUtilisateur profil;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contexte_profil_agence", joinColumns = @JoinColumn(name = "profil_id"))
    @OrderColumn(name = "ordre")
    List<AgenceEmbeddable> agences = new ArrayList<>();

    protected ProfilAgaEntity() {
    }

    @Embeddable
    public static class AgenceEmbeddable {
        String code;
        String nom;

        protected AgenceEmbeddable() {
        }

        AgenceEmbeddable(String code, String nom) {
            this.code = code;
            this.nom = nom;
        }
    }
}
