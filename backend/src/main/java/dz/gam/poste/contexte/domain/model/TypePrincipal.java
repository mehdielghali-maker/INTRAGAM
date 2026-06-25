package dz.gam.poste.contexte.domain.model;

/**
 * Nature de l'utilisateur connecté : l'ADMIN gère le poste (profils AGA, son compte) et n'a
 * pas accès aux modules métier ; un AGA (ou agent) accède à ses agences et à ses modules.
 */
public enum TypePrincipal {
    ADMIN,
    AGA
}
