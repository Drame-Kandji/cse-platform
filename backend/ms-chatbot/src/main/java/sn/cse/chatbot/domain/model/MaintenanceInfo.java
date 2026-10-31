package sn.cse.chatbot.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maintenance (lecture ms-suivi).
 *
 * @param id identifiant
 * @param numeroOt numéro d'ordre de travail
 * @param materielId matériel
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param type type
 * @param statut statut
 * @param datePlanifiee date planifiée
 * @param dateFin date de fin ou {@code null}
 * @param description description
 * @param cout coût ou {@code null} si masqué
 */
public record MaintenanceInfo(
    String id,
    String numeroOt,
    String materielId,
    String materielCode,
    String materielDesignation,
    String type,
    String statut,
    LocalDate datePlanifiee,
    LocalDate dateFin,
    String description,
    BigDecimal cout) {

  /**
   * @return une copie sans coût
   */
  public MaintenanceInfo sansCout() {
    return new MaintenanceInfo(
        id,
        numeroOt,
        materielId,
        materielCode,
        materielDesignation,
        type,
        statut,
        datePlanifiee,
        dateFin,
        description,
        null);
  }
}
