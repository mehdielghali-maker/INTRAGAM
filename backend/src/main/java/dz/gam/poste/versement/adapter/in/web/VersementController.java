package dz.gam.poste.versement.adapter.in.web;

import dz.gam.poste.contexte.domain.model.ContexteAgence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.contexte.domain.port.in.ConsulterContexteAgenceUseCase;
import dz.gam.poste.versement.adapter.in.web.dto.DeposerVersementRequest;
import dz.gam.poste.versement.adapter.in.web.dto.VersementOptionsResponse;
import dz.gam.poste.versement.adapter.in.web.dto.VersementResponse;
import dz.gam.poste.versement.config.VersementProperties;
import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.SituationMois;
import dz.gam.poste.versement.domain.model.StatutVersement;
import dz.gam.poste.versement.domain.port.in.ConsulterSituationMoisUseCase;
import dz.gam.poste.versement.domain.port.in.ConsulterVersementsUseCase;
import dz.gam.poste.versement.domain.port.in.FiltreVersement;
import dz.gam.poste.versement.domain.port.in.SoumettreVersementUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adapter d'entrée (web) du « Versement bancaire ». L'agence et le créateur sont dérivés du
 * contexte de session (jamais reçus du client) ; la liste et la situation sont bornées à
 * l'agence active.
 */
@RestController
@RequestMapping("/api/versement")
public class VersementController {

    private final SoumettreVersementUseCase soumettre;
    private final ConsulterVersementsUseCase consulter;
    private final ConsulterSituationMoisUseCase situation;
    private final AgenceCouranteQuery agenceCourante;
    private final ConsulterContexteAgenceUseCase contexte;
    private final VersementProperties properties;

    public VersementController(SoumettreVersementUseCase soumettre, ConsulterVersementsUseCase consulter,
                               ConsulterSituationMoisUseCase situation, AgenceCouranteQuery agenceCourante,
                               ConsulterContexteAgenceUseCase contexte, VersementProperties properties) {
        this.soumettre = soumettre;
        this.consulter = consulter;
        this.situation = situation;
        this.agenceCourante = agenceCourante;
        this.contexte = contexte;
        this.properties = properties;
    }

    @GetMapping("/options")
    public VersementOptionsResponse options() {
        List<VersementOptionsResponse.MoisOption> mois = properties.moisDisponibles().stream()
                .map(m -> new VersementOptionsResponse.MoisOption(m, MoisSituation.depuis(m).libelle()))
                .toList();
        return new VersementOptionsResponse(List.copyOf(properties.banques()), mois);
    }

    @GetMapping("/situation")
    public SituationMois situation(@RequestParam String mois) {
        return situation.situation(agenceActive(), MoisSituation.depuis(mois));
    }

    @GetMapping
    public List<VersementResponse> lister(@RequestParam(required = false) StatutVersement statut) {
        FiltreVersement filtre = new FiltreVersement(agenceActive(), statut);
        return consulter.lister(filtre).stream().map(VersementResponse::de).toList();
    }

    @PostMapping("/brouillon")
    @ResponseStatus(HttpStatus.CREATED)
    public VersementResponse enregistrerBrouillon(@Valid @RequestBody DeposerVersementRequest requete) {
        return VersementResponse.de(soumettre.enregistrerBrouillon(agenceActive(), createur(), requete.versCommande()));
    }

    @PostMapping("/soumettre")
    @ResponseStatus(HttpStatus.CREATED)
    public VersementResponse soumettre(@Valid @RequestBody DeposerVersementRequest requete) {
        return VersementResponse.de(soumettre.soumettre(agenceActive(), createur(), requete.versCommande()));
    }

    @PostMapping("/{id}/soumettre")
    public VersementResponse soumettreBrouillon(@PathVariable UUID id) {
        return VersementResponse.de(soumettre.soumettreBrouillon(id));
    }

    private String agenceActive() {
        return agenceCourante.agenceActive().code();
    }

    private String createur() {
        ContexteAgence ctx = contexte.contexte();
        return ctx.utilisateur().nomAffiche();
    }
}
