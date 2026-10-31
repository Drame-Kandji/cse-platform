package sn.cse.achats.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Décision d'approbation (co-validation RM, approbation DG, engagement DAF).
 *
 * @param cibleId appel d'offres ou contrat concerné
 * @param approuve décision
 * @param commentaire commentaire facultatif
 * @param decideur auteur de la décision
 */
public record DecisionCommand(
    UUID cibleId, boolean approuve, String commentaire, CurrentUser decideur) {}
