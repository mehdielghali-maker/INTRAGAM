package dz.gam.poste.contexte.adapter.out.identite;

import dz.gam.poste.contexte.adapter.out.identite.ProfilAgaEntity.AgenceEmbeddable;
import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.model.IdentifiantsAga;
import dz.gam.poste.contexte.domain.port.out.ComptesAgaStore;
import dz.gam.poste.contexte.domain.port.out.MotDePasseEncodeur;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Store des profils SSO mockés (persistés en base), géré par l'administration. Utilise
 * {@link ContexteProperties.Profil} comme type de transfert (mêmes champs que la config).
 * Centralise le hachage du mot de passe (port {@link MotDePasseEncodeur}) : le clair n'est
 * jamais persisté ni retourné. Sert aussi de {@link ComptesAgaStore} pour l'authentification.
 */
@Component
public class ProfilsAdminStore implements ComptesAgaStore {

    private final ProfilAgaJpaRepository repository;
    private final MotDePasseEncodeur encodeur;

    public ProfilsAdminStore(ProfilAgaJpaRepository repository, MotDePasseEncodeur encodeur) {
        this.repository = repository;
        this.encodeur = encodeur;
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

    @Override
    public Optional<IdentifiantsAga> trouverParLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        return repository.findByLogin(login)
                .map(e -> new IdentifiantsAga(e.identifiant, e.login, e.nomAffiche, e.motDePasseHash));
    }

    public ContexteProperties.Profil enregistrer(ContexteProperties.Profil p) {
        ProfilAgaEntity e = repository.findById(p.identifiant()).orElseGet(ProfilAgaEntity::new);
        e.identifiant = p.identifiant();
        e.login = (p.login() == null || p.login().isBlank()) ? null : p.login().trim();
        e.nomAffiche = p.nomAffiche();
        e.profil = p.profil();
        e.agences.clear();
        p.agences().forEach(a -> e.agences.add(new AgenceEmbeddable(a.code(), a.nom())));
        e.modules.clear();
        if (p.modules() != null) {
            e.modules.addAll(p.modules());
        }
        // Mot de passe fourni (création ou changement) → hashé. Vide en édition = inchangé.
        if (p.motDePasse() != null && !p.motDePasse().isBlank()) {
            e.motDePasseHash = encodeur.encoder(p.motDePasse());
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

    /** Transfert de lecture : login exposé, mot de passe (clair ou hash) JAMAIS retourné. */
    private static ContexteProperties.Profil versProfil(ProfilAgaEntity e) {
        List<ContexteProperties.Agence> agences = e.agences.stream()
                .map(a -> new ContexteProperties.Agence(a.code, a.nom))
                .toList();
        return new ContexteProperties.Profil(e.identifiant, e.login, e.nomAffiche, e.profil, agences,
                List.copyOf(e.modules), null);
    }
}
