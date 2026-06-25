package dz.gam.poste.cheque.adapter.in.web;

import dz.gam.poste.cheque.adapter.in.web.dto.DossierChequeResponse;
import dz.gam.poste.cheque.adapter.in.web.dto.FaireAvancerStatutRequest;
import dz.gam.poste.cheque.domain.model.StatutCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FaireAvancerStatutUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adapter d'entrée (web) : expose le suivi des chèques en REST. Il ne fait que traduire
 * HTTP ↔ use cases ; aucune règle métier ici.
 */
@RestController
@RequestMapping("/api/cheques")
public class ChequeController {

    private final ConsulterDossiersUseCase consulter;
    private final FaireAvancerStatutUseCase faireAvancer;
    private final AgenceCouranteQuery agenceCourante;

    public ChequeController(ConsulterDossiersUseCase consulter, FaireAvancerStatutUseCase faireAvancer,
                           AgenceCouranteQuery agenceCourante) {
        this.consulter = consulter;
        this.faireAvancer = faireAvancer;
        this.agenceCourante = agenceCourante;
    }

    @GetMapping
    public List<DossierChequeResponse> lister(@RequestParam(required = false) StatutCheque statut) {
        // Agence dérivée du contexte : l'agence active ; toutes (filtre nul) en mode consolidé.
        String agence = agenceCourante.estConsolide() ? null : agenceCourante.agencePourAction().code();
        return consulter.lister(FiltreDossier.de(statut, agence))
                .stream()
                .map(DossierChequeResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    public DossierChequeResponse obtenir(@PathVariable UUID id) {
        return DossierChequeResponse.de(consulter.obtenir(id));
    }

    @PostMapping("/{id}/statut")
    public DossierChequeResponse faireAvancerStatut(@PathVariable UUID id,
                                                    @Valid @RequestBody FaireAvancerStatutRequest requete) {
        agenceCourante.agencePourAction(); // avancement interdit en mode consolidé (409)
        return DossierChequeResponse.de(faireAvancer.faireAvancer(id, requete.statutCible()));
    }
}
