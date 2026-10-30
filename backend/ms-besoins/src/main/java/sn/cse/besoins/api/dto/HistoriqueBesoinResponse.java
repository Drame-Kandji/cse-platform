package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import sn.cse.besoins.domain.model.ActionHistorique;
import sn.cse.besoins.domain.model.HistoriqueBesoin;
import sn.cse.besoins.domain.model.StatutBesoin;

/**
 * Ressource {@code HistoriqueBesoin}.
 *
 * @param id identifiant
 * @param statut statut après l'action
 * @param action action
 * @param auteurId auteur
 * @param auteurNom nom de l'auteur
 * @param commentaire commentaire
 * @param date instant
 */
@Schema(name = "HistoriqueBesoin", description = "Entrée de l'historique d'un besoin")
public record HistoriqueBesoinResponse(
    @Schema(example = "7a1c2d3e-4f50-4617-8293-a4b5c6d7e8f9") UUID id,
    @Schema(example = "VALIDE") StatutBesoin statut,
    @Schema(example = "VALIDATION") ActionHistorique action,
    @Schema(example = "00000000-0000-0000-0000-000000000201", nullable = true) UUID auteurId,
    @Schema(example = "Ibrahima Fall") String auteurNom,
    @Schema(example = "Validé : priorité au lot terrassement", nullable = true) String commentaire,
    @Schema(example = "2026-10-30T09:15:00Z") Instant date) {

  /**
   * @param h entrée du domaine
   * @return la ressource
   */
  public static HistoriqueBesoinResponse depuis(HistoriqueBesoin h) {
    return new HistoriqueBesoinResponse(
        h.id(), h.statut(), h.action(), h.auteurId(), h.auteurNom(), h.commentaire(), h.date());
  }
}
