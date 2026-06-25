package dz.gam.poste.versement.domain.model;

import java.math.BigDecimal;

/**
 * Vue LECTURE SEULE de la situation financière d'un mois pour une agence : production
 * émise + encaissé (PROASSUR), déjà versé en banque (Sage), reste à régulariser.
 *
 * <p>Le « reste à régulariser » = encaissé − versé : MÊME définition et MÊME source que le
 * KPI « Écart à régulariser » de l'accueil (calcul partagé, pas de divergence).
 * Le poste lit et combine, il ne recalcule jamais la vérité comptable.
 */
public record SituationMois(
        String codeAgence,
        String moisValeur,
        String moisLibelle,
        BigDecimal productionEmise,
        BigDecimal encaisse,
        BigDecimal dejaVerse,
        BigDecimal resteARegulariser,
        BigDecimal pourcentage,
        boolean aRegulariser) {
}
