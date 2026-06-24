package dz.gam.poste.dpd.domain.model;

/**
 * Informations client. Pré-remplies depuis PROASSUR à partir de la proposition, puis
 * modifiables par l'agence si besoin (elles restent propres au dossier de suivi du poste).
 */
public record InfoClient(
        String nomAssure,
        String nomSouscripteur,
        String telephone,
        String cnrc,
        TypePersonne typePersonne,
        boolean institutionPublique,
        String adresse) {
}
