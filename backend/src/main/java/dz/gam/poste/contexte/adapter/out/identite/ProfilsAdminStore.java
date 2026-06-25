package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.adapter.out.identite.ProfilAgaEntity.AgenceEmbeddable;
import dz.gam.poste.contexte.config.ContexteProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Store des profils SSO mockés (persistés en base), géré par l'administration. Utilise
 * {@link ContexteProperties.Profil} comme type de transfert (mêmes champs que la config).
 */
@Component
public class ProfilsAdminStore {

    private final ProfilAgaJpaRepository repository;

    public ProfilsAdminStore(ProfilAgaJpaRepository repository) {
        this.repository = repository;
    }

    public boolean estVide() {
        return repository.count() == 0;
    }

    public List<ContexteProperties.Profil> lister() {
        return repository.findAll().stream().map(ProfilsAdminStore::versProfil).toList();
    }

    public Optional<ContexteProperties.Profil> trouver(String identifiant) {
        return repository.findById(identifiant).map(ProfilsAdminStore::versProfil);
    }

    public ContexteProperties.Profil enregistrer(ContexteProperties.Profil p) {
        ProfilAgaEntity e = repository.findById(p.identifiant()).orElseGet(ProfilAgaEntity::new);
        e.identifiant = p.identifiant();
        e.nomAffiche = p.nomAffiche();
        e.profil = p.profil();
        e.agences.clear();
        p.agences().forEach(a -> e.agences.add(new AgenceEmbeddable(a.code(), a.nom())));
        e.modules.clear();
        if (p.modules() != null) {
            e.modules.addAll(p.modules());
        }
        return versProfil(repository.save(e));
    }

    public void supprimer(String identifiant) {
        repository.deleteById(identifiant);
    }

    /** Tous les codes d'agences déclarés (toutes profils confondus). */
    public List<String> tousLesCodesAgences() {
        Set<String> codes = new LinkedHashSet<>();
        repository.findAll().forEach(e -> e.agences.forEach(a -> codes.add(a.code)));
        return List.copyOf(codes);
    }

    private static ContexteProperties.Profil versProfil(ProfilAgaEntity e) {
        List<ContexteProperties.Agence> agences = e.agences.stream()
                .map(a -> new ContexteProperties.Agence(a.code, a.nom))
                .toList();
        return new ContexteProperties.Profil(e.identifiant, e.nomAffiche, e.profil, agences,
                List.copyOf(e.modules));
    }
}
