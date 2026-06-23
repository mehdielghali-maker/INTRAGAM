package dz.gam.poste.cheque.domain.event;

import dz.gam.poste.cheque.domain.model.ReferenceCheque;
import dz.gam.poste.cheque.domain.model.StatutCheque;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement de domaine produit par l'agrégat {@code DossierCheque} quand il atteint
 * un statut terminal (ENCAISSE ou RETOURNE).
 *
 * <p>C'est un objet métier pur : sa <em>publication</em> vers le bus est un détail
 * d'infrastructure assuré par un port de sortie (voir {@code PublicationEvenementPort}).
 * Il matérialise la boucle fermée (ADR 0003).
 */
public record ChequeStatutFinaliseEvent(
        ReferenceCheque reference,
        StatutCheque statutFinal,
        Instant dateFinalisation) {

    public ChequeStatutFinaliseEvent {
        Objects.requireNonNull(reference, "reference obligatoire");
        Objects.requireNonNull(statutFinal, "statutFinal obligatoire");
        Objects.requireNonNull(dateFinalisation, "dateFinalisation obligatoire");
        if (!statutFinal.estTerminal()) {
            throw new IllegalArgumentException("statutFinal doit être un statut terminal : " + statutFinal);
        }
    }
}
