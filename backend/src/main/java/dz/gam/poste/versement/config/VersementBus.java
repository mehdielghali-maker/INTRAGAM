package dz.gam.poste.versement.config;

/**
 * Noms du bus pour le « Versement bancaire » (même exchange topic que le reste du poste).
 * Sortant : {@code versement.depose} (poste → BPM). Entrant : {@code versement.statut}
 * (BPM → poste).
 */
public final class VersementBus {

    private VersementBus() {
    }

    public static final String EXCHANGE = "gam.poste.events";

    public static final String RK_VERSEMENT_DEPOSE = "versement.depose";
    public static final String RK_VERSEMENT_STATUT = "versement.statut";

    /** File consommée par le BPM (mock) pour traiter le versement déposé. */
    public static final String QUEUE_VERSEMENT_DEPOSE = "bpm.versement-depose";
    /** File consommée par le poste (retours de statut du BPM). */
    public static final String QUEUE_VERSEMENT_STATUT = "poste.versement-statut";
}
