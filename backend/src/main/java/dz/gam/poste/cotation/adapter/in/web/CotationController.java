package dz.gam.poste.cotation.adapter.in.web;

import dz.gam.poste.cotation.adapter.in.web.dto.CreerDemandeRequest;
import dz.gam.poste.cotation.adapter.in.web.dto.DemandeResponse;
import dz.gam.poste.cotation.adapter.in.web.dto.SansSuiteRequest;
import dz.gam.poste.cotation.config.CotationProperties;
import dz.gam.poste.cotation.domain.model.ContexteCotation;
import dz.gam.poste.cotation.domain.model.StatutCotation;
import dz.gam.poste.cotation.domain.port.in.ClotureDemandeUseCase;
import dz.gam.poste.cotation.domain.port.in.ConsulterContexteUseCase;
import dz.gam.poste.cotation.domain.port.in.ConsulterDemandesUseCase;
import dz.gam.poste.cotation.domain.port.in.EnregistrerDemandeUseCase;
import dz.gam.poste.cotation.domain.port.in.FiltreDemande;
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

/** Adapter d'entrée (web) de la « Demande de cotation ». Traduit HTTP ↔ use cases. */
@RestController
@RequestMapping("/api/cotation")
public class CotationController {

    private final EnregistrerDemandeUseCase enregistrer;
    private final ClotureDemandeUseCase cloture;
    private final ConsulterDemandesUseCase consulter;
    private final ConsulterContexteUseCase contexte;
    private final CotationProperties properties;

    public CotationController(EnregistrerDemandeUseCase enregistrer, ClotureDemandeUseCase cloture,
                              ConsulterDemandesUseCase consulter, ConsulterContexteUseCase contexte,
                              CotationProperties properties) {
        this.enregistrer = enregistrer;
        this.cloture = cloture;
        this.consulter = consulter;
        this.contexte = contexte;
        this.properties = properties;
    }

    @GetMapping
    public List<DemandeResponse> lister(@RequestParam(required = false) StatutCotation statut) {
        return consulter.lister(FiltreDemande.parStatut(statut)).stream().map(this::reponse).toList();
    }

    @GetMapping("/{id}")
    public DemandeResponse obtenir(@PathVariable UUID id) {
        return reponse(consulter.obtenir(id));
    }

    @GetMapping("/contexte")
    public ContexteCotation contexte() {
        return contexte.contexte();
    }

    @PostMapping("/brouillon")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeResponse enregistrerBrouillon(@Valid @RequestBody CreerDemandeRequest requete) {
        return reponse(enregistrer.enregistrerBrouillon(requete.versCommande()));
    }

    @PostMapping("/envoyer")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeResponse envoyer(@Valid @RequestBody CreerDemandeRequest requete) {
        return reponse(enregistrer.envoyer(requete.versCommande()));
    }

    @PostMapping("/{id}/envoyer")
    public DemandeResponse envoyerBrouillon(@PathVariable UUID id) {
        return reponse(enregistrer.envoyerBrouillon(id));
    }

    @PostMapping("/{id}/sans-suite")
    public DemandeResponse marquerSansSuite(@PathVariable UUID id, @RequestBody(required = false) SansSuiteRequest requete) {
        String motif = requete == null ? null : requete.motif();
        return reponse(cloture.marquerSansSuite(id, motif));
    }

    private DemandeResponse reponse(dz.gam.poste.cotation.domain.model.DemandeCotation d) {
        return DemandeResponse.de(d, properties.libelles());
    }
}
