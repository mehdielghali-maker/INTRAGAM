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
     * Vue consolidée : agrège les indicateurs sur les agences données (tout le périmètre, ou
     * les sous-agences d'un groupe) et fournit la répartition par agence. Lecture seule.
     *
     * @param entete identité affichée de l'ensemble consolidé (nom + code de la sélection)
     */
    TableauBord consolider(Periode periode, List<InfoAgence> agences, InfoAgence entete);
}
