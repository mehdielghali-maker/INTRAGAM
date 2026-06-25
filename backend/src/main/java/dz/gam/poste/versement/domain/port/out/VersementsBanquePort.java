package dz.gam.poste.versement.domain.port.out;

import dz.gam.poste.versement.domain.model.MoisSituation;

import java.math.BigDecimal;

/**
 * Port de sortie vers Sage (comptabilité) : montant déjà versé en banque d'un mois pour
 * une agence. Mock en dev ; remplaçable par l'API réelle Sage.
 */
public interface VersementsBanquePort {

    BigDecimal montantDejaVerse(String codeAgence, MoisSituation mois);
}
