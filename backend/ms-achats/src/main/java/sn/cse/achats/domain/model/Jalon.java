package sn.cse.achats.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Jalon d'exécution d'un contrat. Son statut se déduit des dates : atteint s'il a une date réelle,
 * en retard si l'échéance est dépassée, à venir sinon.
 *
 * @param id identifiant
 * @param libelle libellé
 * @param datePrevue échéance
 * @param dateReelle date de réalisation
 */
public record Jalon(UUID id, String libelle, LocalDate datePrevue, LocalDate dateReelle) {

  public Jalon {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(libelle, "libelle");
    Objects.requireNonNull(datePrevue, "datePrevue");
  }

  /**
   * @param aujourdhui date de référence
   * @return le statut à cette date
   */
  public StatutJalon statutAu(LocalDate aujourdhui) {
    if (dateReelle != null) {
      return StatutJalon.ATTEINT;
    }
    return aujourdhui.isAfter(datePrevue) ? StatutJalon.EN_RETARD : StatutJalon.A_VENIR;
  }

  Jalon atteintLe(LocalDate date) {
    return new Jalon(id, libelle, datePrevue, date);
  }
}
