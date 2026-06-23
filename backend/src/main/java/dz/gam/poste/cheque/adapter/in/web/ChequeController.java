package dz.gam.poste.cheque.adapter.in.web;

import dz.gam.poste.cheque.adapter.in.web.dto.DossierChequeResponse;
import dz.gam.poste.cheque.adapter.in.web.dto.FaireAvancerStatutRequest;
import dz.gam.poste.cheque.domain.model.StatutCheque;
import dz.gam.poste.cheque.domain.port.in.ConsulterDossiersUseCase;
import dz.gam.poste.cheque.domain.port.in.FaireAvancerStatutUseCase;
import dz.gam.poste.cheque.domain.port.in.FiltreDossier;
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

    public ChequeController(ConsulterDossiersUseCase consulter, FaireAvancerStatutUseCase faireAvancer) {
        this.consulter = consulter;
        this.faireAvancer = faireAvancer;
    }

    @GetMapping
    public List<DossierChequeResponse> lister(
            @RequestParam(required = false) StatutCheque statut,
            @RequestParam(required = false) String agence) {
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
        return DossierChequeResponse.de(faireAvancer.faireAvancer(id, requete.statutCible()));
    }
}
