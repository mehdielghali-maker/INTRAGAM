package dz.gam.poste.tableaubord.domain.model;

/**
 * Fenêtre de période du tableau de bord. Choisie par l'utilisateur via le sélecteur
 * de la barre supérieure. C'est un paramètre de lecture transmis aux adapters : le
 * poste ne recalcule rien, il demande les mesures pour la période voulue.
 */
public enum Periode {

    MOIS_COURANT("Mois courant"),
    YTD("YTD");

    private final String libelle;

    Periode(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
