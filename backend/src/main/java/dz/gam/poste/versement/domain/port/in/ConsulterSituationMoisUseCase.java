package dz.gam.poste.versement.domain.port.in;

import dz.gam.poste.versement.domain.model.MoisSituation;
import dz.gam.poste.versement.domain.model.SituationMois;

/** Port d'entrée : situation financière du mois (lecture seule) pour l'agence active. */
public interface ConsulterSituationMoisUseCase {

    SituationMois situation(String codeAgence, MoisSituation mois);
}
