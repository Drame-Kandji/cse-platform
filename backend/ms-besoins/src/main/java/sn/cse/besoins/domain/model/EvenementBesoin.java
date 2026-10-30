package sn.cse.besoins.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement de domaine en attente de publication.
 *
 * @param type nature de l'événement
 * @param date instant de survenue
 */
public record EvenementBesoin(TypeEvenementBesoin type, Instant date) {

  public EvenementBesoin {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(date, "date");
  }
}
