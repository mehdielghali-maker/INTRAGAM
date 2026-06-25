package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.domain.model.CompteAdmin;
import dz.gam.poste.contexte.domain.port.out.CompteAdminStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Persistance JPA du compte d'administration unique (table {@code contexte_compte_admin}). */
@Component
public class CompteAdminStoreJpa implements CompteAdminStore {

    private final CompteAdminJpaRepository repository;

    public CompteAdminStoreJpa(CompteAdminJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompteAdmin> charger() {
        return repository.findAll().stream().findFirst()
                .map(e -> new CompteAdmin(e.login, e.motDePasseHash, e.emailRecuperation));
    }

    @Override
    public void enregistrer(CompteAdmin compte) {
        CompteAdminEntity e = repository.findById(compte.login()).orElseGet(CompteAdminEntity::new);
        e.login = compte.login();
        e.motDePasseHash = compte.motDePasseHash();
        e.emailRecuperation = compte.emailRecuperation();
        repository.save(e);
    }
}
