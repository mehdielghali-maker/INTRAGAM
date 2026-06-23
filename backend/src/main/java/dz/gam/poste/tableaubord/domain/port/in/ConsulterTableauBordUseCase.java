package dz.gam.poste.tableaubord.domain.port.in;

import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.TableauBord;

/** Port d'entrée : consulter le tableau de bord de pilotage de l'agence. */
public interface ConsulterTableauBordUseCase {

    /** @param periode période demandée ; si {@code null}, la période par défaut de config est utilisée */
    TableauBord consulter(Periode periode);
}
