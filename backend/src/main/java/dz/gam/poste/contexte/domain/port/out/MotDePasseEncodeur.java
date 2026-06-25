package dz.gam.poste.contexte.domain.port.out;

/**
 * Port de sortie : encodage et vérification des mots de passe. Garde le domaine indépendant
 * de l'implémentation cryptographique (BCrypt côté adapter).
 */
public interface MotDePasseEncodeur {

    /** Empreinte (salée) du mot de passe en clair, à stocker. */
    String encoder(String motDePasseClair);

    /** Vrai si le mot de passe en clair correspond à l'empreinte donnée. */
    boolean correspond(String motDePasseClair, String empreinte);
}
