package sn.cse.reporting.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Demande de tableau de bord ({@code GET /api/reporting/kpis} et {@code /export}).
 *
 * @param utilisateur utilisateur courant
 * @param profil profil demandé (facultatif : profil principal)
 * @param du début (facultatif)
 * @param au fin (facultatif)
 * @param chantierId filtre de chantier (facultatif)
 */
public record DemandeTableau(
    CurrentUser utilisateur, String profil, LocalDate du, LocalDate au, UUID chantierId) {

  public DemandeTableau {
    Objects.requireNonNull(utilisateur, "utilisateur");
  }
}
