package dz.gam.poste.cotation.adapter.in.web;

import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adapter d'entrée (web) de la « Demande de cotation ». Traduit HTTP ↔ use cases. */
@RestController
@RequestMapping("/api/cotation")
public class CotationController {

    private final EnregistrerDemandeUseCase enregistrer;
    private final ClotureDemandeUseCase cloture;
    private final ConsulterDemandesUseCase consulter;
    private final ConsulterContexteUseCase contexte;
    private final AgenceCouranteQuery agenceCourante;
    private final CotationProperties properties;

    public CotationController(EnregistrerDemandeUseCase enregistrer, ClotureDemandeUseCase cloture,
                              ConsulterDemandesUseCase consulter, ConsulterContexteUseCase contexte,
                              AgenceCouranteQuery agenceCourante, CotationProperties properties) {
        this.enregistrer = enregistrer;
        this.cloture = cloture;
        this.consulter = consulter;
        this.contexte = contexte;
        this.agenceCourante = agenceCourante;
        this.properties = properties;
    }

    @GetMapping
    public List<DemandeResponse> lister(@RequestParam(required = false) StatutCotation statut) {
        // Liste bornée à l'agence active (ou à tout le périmètre en mode consolidé).
        Set<String> agences = agenceCourante.agencesActives().stream()
                .map(Agence::code).collect(Collectors.toSet());
        return consulter.lister(FiltreDemande.parStatut(statut)).stream()
                .filter(d -> agences.contains(d.codeAgence()))
                .map(this::reponse).toList();
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
        agenceCourante.agencePourAction(); // interdit en consolidé (409) ; l'agence vient du contexte
        return reponse(enregistrer.enregistrerBrouillon(requete.versCommande()));
    }

    @PostMapping("/envoyer")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeResponse envoyer(@Valid @RequestBody CreerDemandeRequest requete) {
        agenceCourante.agencePourAction();
        return reponse(enregistrer.envoyer(requete.versCommande()));
    }

    @PostMapping("/{id}/envoyer")
    public DemandeResponse envoyerBrouillon(@PathVariable UUID id) {
        agenceCourante.agencePourAction();
        return reponse(enregistrer.envoyerBrouillon(id));
    }

    @PostMapping("/{id}/sans-suite")
    public DemandeResponse marquerSansSuite(@PathVariable UUID id, @RequestBody(required = false) SansSuiteRequest requete) {
        agenceCourante.agencePourAction();
        String motif = requete == null ? null : requete.motif();
        return reponse(cloture.marquerSansSuite(id, motif));
    }

    private DemandeResponse reponse(dz.gam.poste.cotation.domain.model.DemandeCotation d) {
        return DemandeResponse.de(d, properties.libelles());
    }
}
