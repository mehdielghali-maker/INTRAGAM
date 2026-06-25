package dz.gam.poste.contexte.domain.model;

/**
 * Profil de l'utilisateur connecté, issu de l'identité SSO. Détermine la taille du
 * périmètre d'agences : un AGA gère plusieurs agences, un agent une seule.
 */
public enum ProfilUtilisateur {
    AGA("Agent Général"),
    AGENT("Agent");

    private final String libelle;

    ProfilUtilisateur(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
