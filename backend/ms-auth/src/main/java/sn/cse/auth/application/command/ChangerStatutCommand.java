package sn.cse.auth.application.command;

import java.util.UUID;

/**
 * Activation ou désactivation d'un compte.
 *
 * @param id compte visé
 * @param actif statut cible
 * @param auteurId administrateur à l'origine de la demande
 */
public record ChangerStatutCommand(UUID id, boolean actif, UUID auteurId) {}
