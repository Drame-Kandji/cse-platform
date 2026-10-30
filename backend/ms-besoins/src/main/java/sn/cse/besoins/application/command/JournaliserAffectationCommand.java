package sn.cse.besoins.application.command;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Affectation notifiée par ms-suivi (événement MATERIEL_AFFECTE) à tracer dans l'historique.
 *
 * @param besoinId besoin référencé
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param chantierNom chantier destinataire
 * @param dateDebut début de l'affectation
 * @param dateFinPrevue fin prévue
 * @param auteurId auteur de l'affectation ({@code null} si système)
 * @param auteurNom nom de l'auteur
 * @param date instant de l'événement
 */
public record JournaliserAffectationCommand(
    UUID besoinId,
    String materielCode,
    String materielDesignation,
    String chantierNom,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    UUID auteurId,
    String auteurNom,
    Instant date) {

  /**
   * @return libellé lisible de l'affectation
   */
  public String libelle() {
    return "Matériel "
        + materielCode
        + " ("
        + materielDesignation
        + ") affecté au chantier "
        + chantierNom
        + " du "
        + dateDebut
        + " au "
        + dateFinPrevue
        + ".";
  }
}
