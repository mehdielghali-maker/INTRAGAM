package dz.gam.poste.attestation.adapter.out.ocr;

import dz.gam.poste.attestation.domain.model.ResultatAttestation;
import dz.gam.poste.attestation.domain.port.out.LectureDocumentPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapter MOCK (dev / tests) : aucune dépendance externe. Actif par défaut
 * (ocr.mode=mock ou absent).
 *
 * Sert des lectures PLAUSIBLES et VARIÉES (formats du contrat : police 15 chiffres,
 * quittance 8 chiffres, dates JJ/MM/AAAA, prime « 2400,97 », agence NN.AA.NNNN) en
 * alternant lectures fiables (« lu ») et douteuses (« a_verifier », champs manquants)
 * pour exercer les deux parcours du front (confirmation directe / correction).
 */
@Component
@ConditionalOnProperty(name = "ocr.mode", havingValue = "mock", matchIfMissing = true)
public class LectureDocumentMockAdapter implements LectureDocumentPort {

    /** Échantillons cyclés à chaque appel : chaque scan « lit » une attestation différente. */
    private static final List<ResultatAttestation> ECHANTILLONS = List.of(
            new ResultatAttestation(
                    "160312202600418", "26004181", "00123-316-16", "BENALI Karim",
                    "01/01/2026", "31/12/2026", "24800,50", "16.AL.0316",
                    0.94, ResultatAttestation.STATUT_LU,
                    "ATTESTATION D'ASSURANCE\nPolice 160312202600418 Quittance 26004181\n"
                            + "Assuré : BENALI Karim — Véhicule 00123-316-16\n"
                            + "Valide du 01/01/2026 au 31/12/2026 — Prime TTC 24 800,50 DA"),
            new ResultatAttestation(
                    "310451202600772", "26007725", "04771-131-31", "HAMDANI Louiza",
                    "15/03/2026", "14/09/2026", "13650,00", "31.OR.0045",
                    0.91, ResultatAttestation.STATUT_LU,
                    "ATTESTATION D'ASSURANCE\nPolice 310451202600772 Quittance 26007725\n"
                            + "Assurée : HAMDANI Louiza — Véhicule 04771-131-31\n"
                            + "Valide du 15/03/2026 au 14/09/2026 — Prime TTC 13 650,00 DA"),
            // Lecture DOUTEUSE : quittance et immatriculation non lues → l'AGA doit compléter.
            new ResultatAttestation(
                    "020178202600093", null, null, "MEZIANE Sofiane",
                    "10/06/2026", "09/06/2027", null, "02.HY.0178",
                    0.42, ResultatAttestation.STATUT_A_VERIFIER,
                    "ATTEST..ION D'ASS.RANCE\nPol.ce 020178202600093 Quitt.nce ????\n"
                            + "Assuré : MEZIANE Sofiane — imm. illisible"),
            new ResultatAttestation(
                    "160312202600505", "26005052", "01245-124-16", "SPA TRANS-SUD",
                    "01/07/2026", "30/06/2027", "182400,97", "16.AL.0316",
                    0.88, ResultatAttestation.STATUT_LU,
                    "ATTESTATION D'ASSURANCE — FLOTTE\nPolice 160312202600505 Quittance 26005052\n"
                            + "Assuré : SPA TRANS-SUD — Véhicule 01245-124-16\n"
                            + "Valide du 01/07/2026 au 30/06/2027 — Prime TTC 182 400,97 DA"));

    private final AtomicInteger compteur = new AtomicInteger();

    @Override
    public ResultatAttestation lireAttestation(byte[] photo) {
        return ECHANTILLONS.get(compteur.getAndIncrement() % ECHANTILLONS.size());
    }
}
