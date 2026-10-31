package sn.cse.achats.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Proposition d'attribution (R11, étape 1).
 *
 * @param appelOffreId appel d'offres
 * @param offreId offre proposée
 * @param justification justification
 * @param auteur Responsable des Achats
 */
public record ProposerAttributionCommand(
    UUID appelOffreId, UUID offreId, String justification, CurrentUser auteur) {}
