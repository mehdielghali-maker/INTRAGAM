package dz.gam.poste.contexte.adapter.in.web;

import dz.gam.poste.contexte.adapter.in.web.dto.ChangerAgenceRequest;
import dz.gam.poste.contexte.domain.model.ContexteAgence;
import dz.gam.poste.contexte.domain.port.in.ChangerAgenceActiveUseCase;
import dz.gam.poste.contexte.domain.port.in.ConsulterContexteAgenceUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter d'entrée (web) du contexte d'agence. Expose le contexte (utilisateur, agence
 * active, périmètre) et le changement d'agence active. Aucune donnée n'est filtrée ici :
 * le contrôle de périmètre est réalisé par le service de domaine.
 */
@RestController
@RequestMapping("/api/contexte")
public class ContexteAgenceController {

    private final ConsulterContexteAgenceUseCase consulter;
    private final ChangerAgenceActiveUseCase changer;

    public ContexteAgenceController(ConsulterContexteAgenceUseCase consulter, ChangerAgenceActiveUseCase changer) {
        this.consulter = consulter;
        this.changer = changer;
    }

    @GetMapping
    public ContexteAgence contexte() {
        return consulter.contexte();
    }

    @PostMapping("/agence-active")
    public ContexteAgence changerAgenceActive(@Valid @RequestBody ChangerAgenceRequest requete) {
        return changer.changer(requete.code());
    }
}
