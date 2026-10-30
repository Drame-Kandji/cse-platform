package sn.cse.suivi.application.command;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.domain.model.Maintenance;

/**
 * Planification d'un ordre de travail.
 *
 * @param materielId matériel
 * @param planification détails de l'intervention
 * @param auteur utilisateur
 */
public record PlanifierMaintenanceCommande(
    UUID materielId, Maintenance.Planification planification, CurrentUser auteur) {}
