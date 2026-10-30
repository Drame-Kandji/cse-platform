package sn.cse.fournisseurs.application.command;

import java.util.UUID;
import sn.cse.fournisseurs.domain.model.GraviteLitige;

/**
 * Ouverture d'un litige.
 *
 * @param contratId contrat concerné (optionnel)
 * @param objet objet
 * @param description description
 * @param gravite gravité
 */
public record LitigeCommand(
    UUID contratId, String objet, String description, GraviteLitige gravite) {}
