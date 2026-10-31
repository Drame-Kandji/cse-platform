package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Paramètres métier configurables appliqués aux contrats (issus de {@code cse.achats.*}).
 *
 * @param penaliteRetardPctParJourDefaut pénalité par défaut (% du montant par jour de retard)
 * @param plafondPenalitesPct plafond des pénalités (% du montant)
 * @param tauxTarifJournalier tarif journalier estimé d'un matériel, en fraction de son prix
 *     unitaire (transmis à ms-suivi)
 * @param intervalleRappelRetardJours intervalle minimal entre deux alertes de retard
 */
public record ParametresContrat(
    BigDecimal penaliteRetardPctParJourDefaut,
    BigDecimal plafondPenalitesPct,
    BigDecimal tauxTarifJournalier,
    int intervalleRappelRetardJours) {

  public ParametresContrat {
    Objects.requireNonNull(penaliteRetardPctParJourDefaut, "penaliteRetardPctParJourDefaut");
    Objects.requireNonNull(plafondPenalitesPct, "plafondPenalitesPct");
    Objects.requireNonNull(tauxTarifJournalier, "tauxTarifJournalier");
  }
}
