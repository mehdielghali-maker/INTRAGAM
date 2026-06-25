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

    /** Login de connexion saisi par l'AGA (distinct de l'identifiant technique). Unique ;
     *  null tant qu'aucun login n'a été défini en admin (ce profil ne peut alors pas se connecter). */
    @Column(unique = true)
    String login;

    /** Empreinte BCrypt du mot de passe ; null = pas de mot de passe (connexion impossible). */
    @Column
    String motDePasseHash;

    @Column(nullable = false)
    String nomAffiche;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    ProfilUtilisateur profil;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contexte_profil_agence", joinColumns = @JoinColumn(name = "profil_id"))
    @OrderColumn(name = "ordre")
    List<AgenceEmbeddable> agences = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contexte_profil_module", joinColumns = @JoinColumn(name = "profil_id"))
    @Column(name = "module")
    @OrderColumn(name = "ordre")
    List<String> modules = new ArrayList<>();

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
