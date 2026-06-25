package dz.gam.poste.tableaubord.adapter.in.web;

import dz.gam.poste.contexte.domain.model.Agence;
import dz.gam.poste.contexte.domain.port.in.AgenceCouranteQuery;
import dz.gam.poste.tableaubord.domain.model.InfoAgence;
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
 * de navigation. L'agence n'est JAMAIS reçue du front : elle est dérivée du contexte de
 * session ({@link AgenceCouranteQuery}), garantie dans le périmètre de l'utilisateur.
 * Les structures renvoyées sont des vues de lecture ; aucune écriture ici.
 */
@RestController
@RequestMapping("/api/agence")
public class TableauBordController {

    private final ConsulterTableauBordUseCase consulterTableauBord;
    private final ConsulterNavigationUseCase consulterNavigation;
    private final AgenceCouranteQuery agenceCourante;

    public TableauBordController(ConsulterTableauBordUseCase consulterTableauBord,
                                 ConsulterNavigationUseCase consulterNavigation,
                                 AgenceCouranteQuery agenceCourante) {
        this.consulterTableauBord = consulterTableauBord;
        this.consulterNavigation = consulterNavigation;
        this.agenceCourante = agenceCourante;
    }

    @GetMapping("/tableau-bord")
    public TableauBord tableauBord(@RequestParam(required = false) Periode periode) {
        Agence active = agenceCourante.agenceActive();
        return consulterTableauBord.consulter(periode, new InfoAgence(active.nom(), active.code()));
    }

    @GetMapping("/navigation")
    public CompteursAgence navigation() {
        return consulterNavigation.badges();
    }
}
