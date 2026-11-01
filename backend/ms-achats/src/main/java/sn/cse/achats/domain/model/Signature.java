package sn.cse.achats.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Signature d'un contrat par l'une des parties (CSE ou fournisseur).
 *
 * @param parId identifiant Keycloak du signataire ({@code null} pour une signature système)
 * @param parNom nom du signataire
 * @param date instant de la signature
 */
public record Signature(UUID parId, String parNom, Instant date) {

  public Signature {
    Objects.requireNonNull(parNom, "parNom");
    Objects.requireNonNull(date, "date");
  }

  /**
   * @param signataire auteur de la signature
   * @param date instant de la signature
   * @return la signature
   */
  public static Signature par(Acteur signataire, Instant date) {
    return new Signature(signataire.id(), signataire.nom(), date);
  }
}
