package dz.gam.poste.versement.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Commande de dépôt d'un versement. Le code agence et le créateur n'y figurent PAS :
 * ils sont dérivés du contexte de session côté serveur (sécurité).
 *
 * @param moisSituation      mois à justifier (AAAA-MM)
 * @param montantVerse       montant versé en banque (DA)
 * @param dateVersement      date du versement
 * @param referenceBordereau référence du bordereau (facultatif)
 * @param banque             banque (facultatif)
 * @param commentaire        commentaire (facultatif)
 * @param nomsFichiers       noms des pièces (reçu) à déposer en GED
 */
public record DeposerVersementCommand(
        String moisSituation,
        BigDecimal montantVerse,
        LocalDate dateVersement,
        String referenceBordereau,
        String banque,
        String commentaire,
        List<String> nomsFichiers) {
}
