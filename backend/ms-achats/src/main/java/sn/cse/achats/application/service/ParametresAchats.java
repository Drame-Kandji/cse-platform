package sn.cse.achats.application.service;

import java.math.BigDecimal;
import java.util.Objects;
import sn.cse.achats.domain.model.ParametresContrat;

/**
 * Paramètres métier configurables du service ({@code cse.achats.*}).
 *
 * @param seuilDelegation montant (FCFA) au-delà duquel l'attribution exige l'approbation DG (R11)
 * @param contrat paramètres des contrats (pénalités, tarif journalier, rappels)
 */
public record ParametresAchats(BigDecimal seuilDelegation, ParametresContrat contrat) {

  public ParametresAchats {
    Objects.requireNonNull(seuilDelegation, "seuilDelegation");
    Objects.requireNonNull(contrat, "contrat");
  }
}
