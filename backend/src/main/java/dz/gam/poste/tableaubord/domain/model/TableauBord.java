package dz.gam.poste.tableaubord.domain.model;

import java.util.List;

/**
 * Vue de pilotage de l'accueil agence. Assemble les 5 KPI, la stat secondaire
 * (contrats actifs), le bloc « Coup d'œil » et le bloc « Production & dépôts ».
 *
 * <p>Toutes les valeurs proviennent des systèmes de référence via les ports ; le poste
 * ne fait que les présenter et en dériver des indicateurs de présentation (variations,
 * écart, seuil).
 */
public record TableauBord(
        InfoAgence agence,
        String periodeLibelle,
        String dateLibelle,
        int contratsActifs,
        int contratsActifsVariation,
        CarteMontant caYtd,
        CarteMontant caMois,
        CarteEcart ecartDepot,
        CarteMontant creances,
        CarteRatio sp,
        List<CompteurAction> coupDoeil,
        ProductionDepots productionDepots) {
}
