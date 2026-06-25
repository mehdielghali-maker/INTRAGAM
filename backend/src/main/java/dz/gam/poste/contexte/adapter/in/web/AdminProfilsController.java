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

/**
 * Administration des profils SSO (AGA/agents) et de leurs agences — substitut, en attendant
 * Entra ID + le référentiel agences. CRUD persisté ; à l'enregistrement, publie
 * {@link AgencesDeclareesEvent} pour semer les chiffres/chèques de démo des nouvelles agences.
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

    private ContexteProperties.Profil enregistrer(ProfilRequest r) {
        List<ContexteProperties.Agence> agences = r.agences().stream()
                .map(a -> new ContexteProperties.Agence(a.code(), a.nom()))
                .toList();
        ContexteProperties.Profil enregistre = store.enregistrer(
                new ContexteProperties.Profil(r.identifiant(), r.nomAffiche(), r.profil(), agences));
        // Sème les chiffres + chèques de démo des (nouvelles) agences déclarées.
        evenements.publishEvent(new AgencesDeclareesEvent(agences.stream().map(ContexteProperties.Agence::code).toList()));
        return enregistre;
    }

    public record ProfilRequest(
            @NotBlank String identifiant,
            @NotBlank String nomAffiche,
            @NotNull ProfilUtilisateur profil,
            @NotEmpty List<AgenceRequest> agences) {
    }

    public record AgenceRequest(@NotBlank String code, @NotBlank String nom) {
    }
}
