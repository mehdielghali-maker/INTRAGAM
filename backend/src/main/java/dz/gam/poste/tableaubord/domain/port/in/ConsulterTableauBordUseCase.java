package dz.gam.poste.tableaubord.domain.port.in;

import dz.gam.poste.tableaubord.domain.model.InfoAgence;
import dz.gam.poste.tableaubord.domain.model.Periode;
import dz.gam.poste.tableaubord.domain.model.TableauBord;

import java.util.List;

/** Port d'entrée : consulter le tableau de bord de pilotage de l'agence active. */
public interface ConsulterTableauBordUseCase {

    /**
     * @param periode période demandée ; si {@code null}, la période par défaut de config est utilisée
     * @param agence  agence active (héritée du contexte de session) sur laquelle borner les données
     */
    TableauBord consulter(Periode periode, InfoAgence agence);

    /**
     * Vue consolidée (« Toutes mes agences ») : agrège les indicateurs sur tout le périmètre
     * et fournit la répartition par agence. Lecture seule (vue d'ensemble).
     */
    TableauBord consolider(Periode periode, List<InfoAgence> agences);
}
