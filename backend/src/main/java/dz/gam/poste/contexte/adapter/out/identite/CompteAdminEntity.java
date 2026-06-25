package dz.gam.poste.contexte.adapter.out.identite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Compte d'administration unique (ligne identifiée par le login fixe « admin »). */
@Entity
@Table(name = "contexte_compte_admin")
public class CompteAdminEntity {

    @Id
    String login;

    @Column(nullable = false)
    String motDePasseHash;

    @Column
    String emailRecuperation;

    protected CompteAdminEntity() {
    }
}
