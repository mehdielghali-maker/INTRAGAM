package dz.gam.poste.contexte.domain.port.out;

import dz.gam.poste.contexte.domain.model.CompteAdmin;

import java.util.Optional;

/**
 * Port de sortie : persistance du compte d'administration unique. {@link #charger()} renvoie
 * vide tant qu'il n'a pas été initialisé (le seeding crée alors admin/admin).
 */
public interface CompteAdminStore {

    Optional<CompteAdmin> charger();

    void enregistrer(CompteAdmin compte);
}
