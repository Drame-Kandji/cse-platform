package sn.cse.fournisseurs.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Demande de blacklistage en attente de décision de la Direction Générale (règle R09, étape 1).
 *
 * @param demandeurId identifiant du Responsable des Achats demandeur
 * @param demandeurNom nom du demandeur
 * @param motif motif de la demande
 * @param date instant de la demande
 */
public record DemandeBlacklistage(
    UUID demandeurId, String demandeurNom, String motif, Instant date) {

  /** Vérifie les champs obligatoires. */
  public DemandeBlacklistage {
    Objects.requireNonNull(demandeurNom, "demandeurNom");
    Objects.requireNonNull(motif, "motif");
    Objects.requireNonNull(date, "date");
  }
}
