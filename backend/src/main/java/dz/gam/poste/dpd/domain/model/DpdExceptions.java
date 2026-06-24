package dz.gam.poste.dpd.domain.model;

/** Exceptions métier du domaine DPD, regroupées. */
public final class DpdExceptions {

    private DpdExceptions() {
    }

    /** Demande introuvable → HTTP 404. */
    public static class DemandeIntrouvable extends RuntimeException {
        public DemandeIntrouvable(String id) {
            super("Aucune demande DPD pour " + id);
        }
    }

    /** Accord introuvable côté PROASSUR → HTTP 404. */
    public static class AccordIntrouvable extends RuntimeException {
        public AccordIntrouvable(String code) {
            super("Aucun accord pour le code " + code);
        }
    }

    /** Transition de statut non autorisée → HTTP 409. */
    public static class TransitionInvalide extends RuntimeException {
        public TransitionInvalide(StatutDpd actuel, StatutDpd cible) {
            super("Transition DPD invalide : " + actuel + " → " + cible
                    + ". Autorisées : " + actuel.prochainsStatuts());
        }
    }

    /** Envoi bloqué faute de Registre de Commerce → HTTP 409. */
    public static class RcManquant extends RuntimeException {
        public RcManquant() {
            super("Le Registre de Commerce (RC) est obligatoire pour envoyer la demande.");
        }
    }
}
