package sn.cse.besoins.domain.validation;

import java.util.Objects;
import java.util.Optional;

/**
 * Résultat du traitement d'une validation par la chaîne.
 *
 * @param etapeApprouvee étape franchie par le validateur
 * @param etapeSuivante prochaine étape requise, vide si la validation est complète
 */
public record DecisionValidation(
    EtapeValidation etapeApprouvee, Optional<EtapeValidation> etapeSuivante) {

  public DecisionValidation {
    Objects.requireNonNull(etapeApprouvee, "etapeApprouvee");
    etapeSuivante = Objects.requireNonNullElse(etapeSuivante, Optional.empty());
  }

  /**
   * @return vrai si toutes les étapes requises sont franchies (le besoin devient VALIDE)
   */
  public boolean estFinale() {
    return etapeSuivante.isEmpty();
  }
}
