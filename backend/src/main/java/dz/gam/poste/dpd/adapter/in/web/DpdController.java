package dz.gam.poste.dpd.adapter.in.web;

import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.dpd.adapter.in.web.dto.AccordSuiviResponse;
import dz.gam.poste.dpd.adapter.in.web.dto.CreerDpdRequest;
import dz.gam.poste.dpd.adapter.in.web.dto.DemandeDpdResponse;
import dz.gam.poste.dpd.adapter.in.web.dto.SynchroniserRequest;
import dz.gam.poste.dpd.config.DpdProperties;
import dz.gam.poste.dpd.domain.model.ContexteDpd;
import dz.gam.poste.dpd.domain.model.DemandeDpd;
import dz.gam.poste.dpd.domain.model.DpdExceptions;
import dz.gam.poste.dpd.domain.model.StatutDpd;
import dz.gam.poste.dpd.domain.port.in.ConsulterDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.EnregistrerDpdUseCase;
import dz.gam.poste.dpd.domain.port.in.FiltreDpd;
import dz.gam.poste.dpd.domain.port.in.MiseAJourDpdUseCase;
import dz.gam.poste.dpd.domain.port.out.AccordProassur;
import dz.gam.poste.dpd.domain.port.out.PrefillProposition;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adapter d'entrée (web) des accords d'échéancier (DPD). */
@RestController
@RequestMapping("/api/dpd")
public class DpdController {

    private final EnregistrerDpdUseCase enregistrer;
    private final ConsulterDpdUseCase consulter;
    private final MiseAJourDpdUseCase miseAJour;
    private final AgenceCouranteQuery agenceCourante;
    private final DpdProperties properties;

    public DpdController(EnregistrerDpdUseCase enregistrer, ConsulterDpdUseCase consulter,
                         MiseAJourDpdUseCase miseAJour, AgenceCouranteQuery agenceCourante,
                         DpdProperties properties) {
        this.enregistrer = enregistrer;
        this.consulter = consulter;
        this.miseAJour = miseAJour;
        this.agenceCourante = agenceCourante;
        this.properties = properties;
    }

    @GetMapping
    public List<DemandeDpdResponse> lister(@RequestParam(required = false) StatutDpd statut) {
        // Liste bornée à l'agence active (ou à tout le périmètre en mode consolidé).
        Set<String> agences = agenceCourante.agencesActives().stream()
                .map(Agence::code).collect(Collectors.toSet());
        return consulter.lister(FiltreDpd.parStatut(statut)).stream()
                .filter(d -> agences.contains(d.codeAgence()))
                .map(this::reponse).toList();
    }

    @GetMapping("/{id}")
    public DemandeDpdResponse obtenir(@PathVariable UUID id) {
        return reponse(consulter.obtenir(id));
    }

    @GetMapping("/contexte")
    public ContexteDpd contexte() {
        return consulter.contexte();
    }

    @GetMapping("/prefill")
    public PrefillProposition prefill(@RequestParam String proposition) {
        return consulter.prefill(proposition)
                .orElseThrow(() -> new DpdExceptions.AccordIntrouvable("proposition " + proposition));
    }

    @PostMapping("/brouillon")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeDpdResponse brouillon(@Valid @RequestBody CreerDpdRequest requete) {
        agenceCourante.agencePourAction(); // interdit en consolidé (409) ; agence issue du contexte
        return reponse(enregistrer.enregistrerBrouillon(requete.versCommande()));
    }

    @PostMapping("/envoyer")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeDpdResponse envoyer(@Valid @RequestBody CreerDpdRequest requete) {
        agenceCourante.agencePourAction();
        return reponse(enregistrer.envoyer(requete.versCommande()));
    }

    @PostMapping("/{id}/envoyer")
    public DemandeDpdResponse envoyerBrouillon(@PathVariable UUID id) {
        agenceCourante.agencePourAction();
        return reponse(enregistrer.envoyerBrouillon(id));
    }

    @GetMapping("/accords/{code}")
    public AccordProassur chargerAccord(@PathVariable String code) {
        return miseAJour.chargerAccord(code).orElseThrow(() -> new DpdExceptions.AccordIntrouvable(code));
    }

    @PostMapping("/accords/{code}/synchroniser")
    public AccordSuiviResponse synchroniser(@PathVariable String code,
                                            @RequestBody(required = false) SynchroniserRequest requete) {
        agenceCourante.agencePourAction();
        String commentaire = requete == null ? null : requete.commentaire();
        return AccordSuiviResponse.de(miseAJour.synchroniser(code, commentaire));
    }

    private DemandeDpdResponse reponse(DemandeDpd d) {
        return DemandeDpdResponse.de(d, properties.libelles());
    }
}
