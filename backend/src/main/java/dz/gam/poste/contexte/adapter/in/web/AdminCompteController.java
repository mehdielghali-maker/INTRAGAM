package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.domain.model.CompteAdminVue;
import dz.gam.poste.contexte.domain.port.in.GererCompteAdminUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion du compte d'administration depuis la session admin : consulter le login et l'e-mail
 * de récupération, changer le mot de passe (avec vérification de l'ancien), définir l'e-mail de
 * récupération. Réservé à l'admin (cf. {@link AuthentificationInterceptor} sur {@code /api/admin/**}).
 */
@RestController
@RequestMapping("/api/admin/compte")
public class AdminCompteController {

    private final GererCompteAdminUseCase compte;

    public AdminCompteController(GererCompteAdminUseCase compte) {
        this.compte = compte;
    }

    @GetMapping
    public CompteAdminVue consulter() {
        return compte.consulter();
    }

    @PutMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerMotDePasse(@Valid @RequestBody ChangerMotDePasseRequest requete) {
        compte.changerMotDePasse(requete.ancien(), requete.nouveau());
    }

    @PutMapping("/email-recuperation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void definirEmail(@Valid @RequestBody EmailRecuperationRequest requete) {
        compte.definirEmailRecuperation(requete.email());
    }

    public record ChangerMotDePasseRequest(@NotBlank String ancien, @NotBlank String nouveau) {
    }

    public record EmailRecuperationRequest(@Email String email) {
    }
}
