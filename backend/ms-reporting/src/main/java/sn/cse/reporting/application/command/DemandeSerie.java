package sn.cse.reporting.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Demande de série temporelle ({@code GET /api/reporting/series}).
 *
 * @param utilisateur utilisateur courant
 * @param indicateur code de l'indicateur
 * @param granularite {@code JOUR}, {@code SEMAINE} ou {@code MOIS} (facultatif : {@code MOIS})
 * @param du début (facultatif)
 * @param au fin (facultatif)
 * @param chantierId filtre de chantier (facultatif)
 */
public record DemandeSerie(
    CurrentUser utilisateur,
    String indicateur,
    String granularite,
    LocalDate du,
    LocalDate au,
    UUID chantierId) {

  public DemandeSerie {
    Objects.requireNonNull(utilisateur, "utilisateur");
  }
}
