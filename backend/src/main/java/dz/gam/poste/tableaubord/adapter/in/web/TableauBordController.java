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

import java.util.List;

/**
 * Adapter d'entrée (web) de l'accueil agence. Expose le tableau de bord et les badges de
 * navigation. L'agence n'est JAMAIS reçue du front : elle est dérivée du contexte de session
 * ({@link AgenceCouranteQuery}). En mode consolidé, l'accueil agrège tout le périmètre.
 */
@RestController
@RequestMapping("/api/agence")
public class TableauBordController {

    private static final InfoAgence CONSOLIDE = new InfoAgence("Toutes mes agences (consolidé)", "CONSOLIDE");

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
        if (agenceCourante.estConsolide()) {
            List<InfoAgence> agences = agenceCourante.agencesActives().stream()
                    .map(a -> new InfoAgence(a.nom(), a.code()))
                    .toList();
            return consulterTableauBord.consolider(periode, agences, CONSOLIDE);
        }
        Agence active = agenceCourante.agencePourAction();
        return consulterTableauBord.consulter(periode, new InfoAgence(active.nom(), active.code()));
    }

    @GetMapping("/navigation")
    public CompteursAgence navigation() {
        return consulterNavigation.badges();
    }
}
