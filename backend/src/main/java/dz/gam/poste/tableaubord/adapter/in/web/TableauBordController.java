package dz.gam.poste.tableaubord.adapter.in.web;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.TableauBord;
import dz.gam.poste.tableaubord.domain.port.in.ConsulterNavigationUseCase;
import dz.gam.poste.tableaubord.domain.port.in.ConsulterTableauBordUseCase;
import dz.gam.poste.tableaubord.domain.port.out.CompteursAgence;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter d'entrée (web) de l'accueil agence. Expose le tableau de bord et les badges
 * de navigation. Les structures renvoyées sont des vues de lecture (présentation) ;
 * aucune écriture, aucun calcul de vérité comptable ici.
 */
@RestController
@RequestMapping("/api/agence")
public class TableauBordController {

    private final ConsulterTableauBordUseCase consulterTableauBord;
    private final ConsulterNavigationUseCase consulterNavigation;

    public TableauBordController(ConsulterTableauBordUseCase consulterTableauBord,
                                 ConsulterNavigationUseCase consulterNavigation) {
        this.consulterTableauBord = consulterTableauBord;
        this.consulterNavigation = consulterNavigation;
    }

    @GetMapping("/tableau-bord")
    public TableauBord tableauBord(@RequestParam(required = false) Periode periode) {
        return consulterTableauBord.consulter(periode);
    }

    @GetMapping("/navigation")
    public CompteursAgence navigation() {
        return consulterNavigation.badges();
    }
}
