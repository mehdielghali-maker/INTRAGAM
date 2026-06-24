package dz.gam.poste.dpd.config;

/**
 * Noms du bus pour le DPD (exchange topic commun du poste).
 * Sortant : {@code dpd.emise} (poste → central), {@code dpd.maj} (poste → Suivi échéanciers).
 * Entrant : {@code dpd.statut} (central → poste).
 */
public final class DpdBus {

    private DpdBus() {
    }

    public static final String EXCHANGE = "gam.poste.events";

    public static final String RK_DEMANDE_EMISE = "dpd.emise";
    public static final String RK_STATUT = "dpd.statut";
    public static final String RK_MAJ = "dpd.maj";

    public static final String QUEUE_DEMANDE_EMISE = "central.dpd-emise";
    public static final String QUEUE_STATUT = "poste.dpd-statut";
    public static final String QUEUE_MAJ = "echeanciers.dpd-maj";
}
