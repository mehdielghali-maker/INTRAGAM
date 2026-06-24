package dz.gam.poste.cotation.config;

/**
 * Noms du bus pour la « Demande de cotation » (même exchange topic que le reste du poste).
 * Sortant : {@code cotation.emise} (poste → central). Entrant : {@code cotation.statut}
 * (central → poste).
 */
public final class CotationBus {

    private CotationBus() {
    }

    public static final String EXCHANGE = "gam.poste.events";

    public static final String RK_DEMANDE_EMISE = "cotation.emise";
    public static final String RK_COTATION_STATUT = "cotation.statut";

    /** File consommée par le mock central (prise en charge + cotation). */
    public static final String QUEUE_DEMANDE_EMISE = "central.cotation-emise";
    /** File consommée par le poste (retours de statut du central). */
    public static final String QUEUE_COTATION_STATUT = "poste.cotation-statut";
}
