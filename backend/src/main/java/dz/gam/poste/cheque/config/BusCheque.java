package dz.gam.poste.cheque.config;

/**
 * Noms partagés du bus pour le périmètre « Suivi des chèques » (voir ADR 0004).
 * Centralisés ici pour que producteurs, consommateurs et déclarations de topologie
 * restent cohérents.
 */
public final class BusCheque {

    private BusCheque() {
    }

    /** Topic exchange unique des événements du poste. */
    public static final String EXCHANGE = "gam.poste.events";

    /** Routing key de l'événement entrant émis par PROASSUR. */
    public static final String RK_CHEQUE_EMIS = "cheque.emis";

    /** Routing key de l'événement terminal publié par le poste (write-back). */
    public static final String RK_CHEQUE_STATUT_FINALISE = "cheque.statut.finalise";

    /** File consommée par le poste pour ouvrir les dossiers de suivi. */
    public static final String QUEUE_CHEQUE_EMIS = "poste.cheque-emis";

    /** File consommée par l'adapter PROASSUR (mock) pour le write-back. */
    public static final String QUEUE_WRITE_BACK = "proassur.write-back";
}
