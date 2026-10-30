package sn.cse.suivi.application.command;

import java.math.BigDecimal;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Clôture d'un ordre de travail.
 *
 * @param maintenanceId ordre de travail
 * @param cout coût total (FCFA)
 * @param observations compte rendu
 * @param heuresCompteur compteur relevé (optionnel)
 * @param auteur utilisateur
 */
public record CloturerMaintenanceCommande(
    UUID maintenanceId,
    BigDecimal cout,
    String observations,
    Long heuresCompteur,
    CurrentUser auteur) {}
