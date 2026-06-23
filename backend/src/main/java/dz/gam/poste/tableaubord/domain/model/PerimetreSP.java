package dz.gam.poste.tableaubord.domain.model;

/**
 * Périmètre du numérateur du ratio S/P (à caler avec la DFC). PARAMÈTRE de config,
 * jamais figé : il détermine ce que l'adapter PROASSUR renvoie comme charge sinistres.
 *
 * <ul>
 *   <li>{@link #REGLES_SEULS} : seulement les sinistres réglés.</li>
 *   <li>{@link #CHARGE_AVEC_PROVISIONS} : charge incluant les provisions.</li>
 * </ul>
 */
public enum PerimetreSP {
    REGLES_SEULS,
    CHARGE_AVEC_PROVISIONS
}
