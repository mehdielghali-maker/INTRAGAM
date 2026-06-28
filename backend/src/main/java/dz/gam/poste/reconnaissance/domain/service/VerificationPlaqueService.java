package dz.gam.poste.reconnaissance.domain.service;

import dz.gam.poste.reconnaissance.domain.model.StatutVerification;
import dz.gam.poste.reconnaissance.domain.port.out.ReconnaissancePort;

/**
 * Comparaison plaque LUE  ↔  immatriculation du CONTRAT.
 *
 * C'est ICI (dans le poste, qui connaît le contrat) que se décide la conformité —
 * pas dans le service RECO. La logique est volontairement tolérante : une lecture
 * incertaine ne doit pas accuser à tort. Domaine pur (aucune dépendance Spring) :
 * câblé en bean dans {@link dz.gam.poste.reconnaissance.config.ReconnaissanceBeansConfig}.
 */
public class VerificationPlaqueService {

    private static String normaliser(String s) {
        return s == null ? null : s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * @param reco         résultat du port reconnaissance
     * @param immatContrat immatriculation pré-remplie depuis PROASSUR
     * @param vue          avant / arriere / gauche / droite / toit
     */
    public StatutVerification evaluer(ReconnaissancePort.ResultatReco reco, String immatContrat, String vue) {
        if (reco == null) {
            return StatutVerification.NON_LUE;
        }
        if (!reco.estVehicule()) {
            return StatutVerification.PAS_UN_VEHICULE;
        }
        boolean vueAvecPlaque = (vue == null) || vue.equals("avant") || vue.equals("arriere");
        if (!vueAvecPlaque) {
            return StatutVerification.VUE_SANS_PLAQUE;
        }
        if (reco.plaque() == null || reco.plaque().isBlank()) {
            return StatutVerification.NON_LUE;
        }
        return normaliser(reco.plaque()).equals(normaliser(immatContrat))
                ? StatutVerification.CONFORME
                : StatutVerification.NON_CONFORME;
    }

    // --- Décision de blocage -------------------------------------------------
    // Par défaut : RIEN n'est bloquant (avertissements seuls). Pour rendre la
    // non-concordance bloquante CÔTÉ AGA (anti-fraude), passer la propriété
    // reco.bloque-non-conforme=true.
    public boolean bloqueValidation(StatutVerification statut, boolean estAga, boolean bloqueNonConforme) {
        return estAga && bloqueNonConforme && statut == StatutVerification.NON_CONFORME;
    }
}
