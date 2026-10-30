package sn.cse.auth.application.query;

/**
 * Bilan d'une synchronisation Keycloak → profils.
 *
 * @param crees profils importés
 * @param modifies profils mis à jour
 * @param desactives profils désactivés (compte absent de Keycloak)
 * @param inchanges profils déjà à jour
 */
public record ResultatSynchronisation(int crees, int modifies, int desactives, int inchanges) {}
