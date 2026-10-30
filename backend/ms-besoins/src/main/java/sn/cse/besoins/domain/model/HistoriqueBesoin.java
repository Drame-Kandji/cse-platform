package sn.cse.besoins.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrée immuable de l'historique d'un besoin : chaque changement est tracé.
 *
 * @param id identifiant
 * @param statut statut du besoin après l'action
 * @param action nature de l'action
 * @param etapeValidation code de l'étape de validation approuvée (sinon {@code null})
 * @param auteurId auteur ({@code null} si système)
 * @param auteurNom nom de l'auteur
 * @param commentaire commentaire libre (motif, détail…)
 * @param date instant de l'action
 */
public record HistoriqueBesoin(
    UUID id,
    StatutBesoin statut,
    ActionHistorique action,
    String etapeValidation,
    UUID auteurId,
    String auteurNom,
    String commentaire,
    Instant date) {

  public HistoriqueBesoin {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(statut, "statut");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(date, "date");
  }

  static HistoriqueBesoin de(
      StatutBesoin statut,
      ActionHistorique action,
      String etape,
      Acteur auteur,
      String commentaire,
      Instant date) {
    return new HistoriqueBesoin(
        UUID.randomUUID(), statut, action, etape, auteur.id(), auteur.nom(), commentaire, date);
  }
}
