package dz.gam.poste.tableaubord.domain.model;

/**
 * Élément actionnable du bloc « Coup d'œil — à traiter ».
 *
 * @param cle    identifiant stable (sert au routage côté UI vers la fonction)
 * @param valeur compteur
 * @param urgent vrai si à traiter en priorité (compteur en rouge)
 */
public record CompteurAction(String cle, int valeur, boolean urgent) {
}
