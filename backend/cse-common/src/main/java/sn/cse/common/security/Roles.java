package sn.cse.common.security;

/**
 * Rôles Keycloak unifiés pour la plateforme CSE.
 */
public final class Roles {
    private Roles() {}

    public static final String CHEF_CHANTIER = "CHEF_CHANTIER";
    public static final String RESPONSABLE_MATERIEL = "RESPONSABLE_MATERIEL";
    public static final String RESPONSABLE_ACHATS = "RESPONSABLE_ACHATS";
    public static final String DAF = "DAF";
    public static final String DIRECTION_GENERALE = "DIRECTION_GENERALE";
    public static final String ADMIN_SYSTEME = "ADMIN_SYSTEME";
    public static final String FOURNISSEUR = "FOURNISSEUR";
}
