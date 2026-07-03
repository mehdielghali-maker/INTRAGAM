package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.adapter.out.identite.ProfilsAdminStore;
import dz.gam.poste.contexte.config.ContexteProperties;
import dz.gam.poste.contexte.domain.event.AgencesDeclareesEvent;
import dz.gam.poste.contexte.domain.model.ProfilUtilisateur;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Administration des profils SSO (AGA/agents), de leurs agences et de leurs identifiants de
 * connexion (login + mot de passe) — substitut, en attendant Entra ID + le référentiel agences.
 * CRUD persisté ; à l'enregistrement, publie {@link AgencesDeclareesEvent} pour semer les
 * chiffres/chèques de démo des nouvelles agences. Réservé à l'admin (cf. interceptor).
 */
@RestController
@RequestMapping("/api/admin/profils")
public class AdminProfilsController {

    private final ProfilsAdminStore store;
    private final ApplicationEventPublisher evenements;

    public AdminProfilsController(ProfilsAdminStore store, ApplicationEventPublisher evenements) {
        this.store = store;
        this.evenements = evenements;
    }

    @GetMapping
    public List<ContexteProperties.Profil> lister() {
        return store.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContexteProperties.Profil creer(@Valid @RequestBody ProfilRequest requete) {
        if (store.trouver(requete.identifiant()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Identifiant déjà utilisé : " + requete.identifiant());
        }
        if (requete.motDePasse() == null || requete.motDePasse().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est requis à la création");
        }
        verifierLoginDisponible(requete.login(), requete.identifiant());
        return enregistrer(requete);
    }

    @PutMapping("/{identifiant}")
    public ContexteProperties.Profil modifier(@PathVariable String identifiant,
                                              @Valid @RequestBody ProfilRequest requete) {
        if (!identifiant.equals(requete.identifiant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant ne peut pas changer");
        }
        if (store.trouver(identifiant).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil inconnu : " + identifiant);
        }
        verifierLoginDisponible(requete.login(), identifiant);
        return enregistrer(requete);
    }

    @DeleteMapping("/{identifiant}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable String identifiant) {
        if (store.lister().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de supprimer le dernier profil");
        }
        store.supprimer(identifiant);
    }

    /** Le login doit être libre (ou déjà détenu par ce même profil). */
    private void verifierLoginDisponible(String login, String identifiant) {
        if (login == null || login.isBlank()) {
            return;
        }
        Optional<String> proprietaire = store.trouverParLogin(login.trim())
                .map(dz.gam.poste.contexte.domain.model.IdentifiantsAga::identifiant);
        if (proprietaire.isPresent() && !proprietaire.get().equals(identifiant)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login déjà utilisé : " + login);
        }
    }

    private ContexteProperties.Profil enregistrer(ProfilRequest r) {
        List<ContexteProperties.Agence> agences = r.agences().stream()
                .map(a -> new ContexteProperties.Agence(a.code(), a.nom()))
                .toList();
        List<String> modules = r.modules() == null ? List.of() : r.modules();
        ContexteProperties.Profil enregistre = store.enregistrer(
                new ContexteProperties.Profil(r.identifiant(), r.login(), r.nomAffiche(), r.profil(),
                        agences, modules, r.motDePasse()));
        // Sème les données de démo des (nouvelles) agences déclarées. Un échec du seeding ne doit
        // pas faire échouer l'enregistrement du profil (les listeners idempotents rattraperont).
        try {
            evenements.publishEvent(new AgencesDeclareesEvent(agences.stream().map(ContexteProperties.Agence::code).toList()));
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(AdminProfilsController.class)
                    .warn("Seeding de démo différé après enregistrement du profil : {}", e.getMessage());
        }
        return enregistre;
    }

    /**
     * @param login      login de connexion (requis)
     * @param motDePasse mot de passe en clair : requis à la création, facultatif en modification
     *                   (vide = mot de passe inchangé)
     */
    public record ProfilRequest(
            @NotBlank String identifiant,
            @NotBlank String login,
            @NotBlank String nomAffiche,
            @NotNull ProfilUtilisateur profil,
            @NotEmpty List<AgenceRequest> agences,
            List<String> modules,
            String motDePasse) {
    }

    public record AgenceRequest(@NotBlank String code, @NotBlank String nom) {
    }
}
