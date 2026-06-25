package dz.gam.poste.versement.adapter.in.web.dto;

import java.util.List;

/** Options d'écran : banques et mois sélectionnables (issus de la configuration). */
public record VersementOptionsResponse(List<String> banques, List<MoisOption> mois) {

    public record MoisOption(String valeur, String libelle) {
    }
}
