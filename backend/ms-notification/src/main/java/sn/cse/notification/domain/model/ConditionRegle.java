package sn.cse.notification.domain.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Condition supplémentaire d'application d'une règle, évaluée sur un champ du payload (ex. {@code
 * gravite EGAL BLOQUANTE}, {@code scorePerformance INFERIEUR 60}).
 *
 * @param champ chemin du champ
 * @param operateur opérateur
 * @param valeur valeur de comparaison (ignorée pour VRAI, FAUX, PRESENT)
 */
public record ConditionRegle(String champ, Operateur operateur, String valeur) {

  public ConditionRegle {
    Objects.requireNonNull(champ, "champ");
    Objects.requireNonNull(operateur, "operateur");
    boolean valeurRequise =
        operateur == Operateur.EGAL
            || operateur == Operateur.DIFFERENT
            || operateur == Operateur.INFERIEUR
            || operateur == Operateur.SUPERIEUR
            || operateur == Operateur.COMMENCE_PAR;
    if (valeurRequise && (valeur == null || valeur.isBlank())) {
      throw new IllegalArgumentException(
          "La condition sur « " + champ + " » exige une valeur pour " + operateur);
    }
  }

  /**
   * @param evenement événement reçu
   * @return vrai si la condition est satisfaite
   */
  public boolean estSatisfaite(EvenementMetier evenement) {
    Optional<String> brute = evenement.valeur(champ).map(String::valueOf).filter(s -> !s.isBlank());
    return switch (operateur) {
      case EGAL -> brute.map(v -> v.equalsIgnoreCase(valeur)).orElse(false);
      case DIFFERENT -> brute.map(v -> !v.equalsIgnoreCase(valeur)).orElse(true);
      case INFERIEUR ->
          nombre(brute).map(n -> n.compareTo(new BigDecimal(valeur)) < 0).orElse(false);
      case SUPERIEUR ->
          nombre(brute).map(n -> n.compareTo(new BigDecimal(valeur)) > 0).orElse(false);
      case VRAI -> brute.map(v -> "true".equals(v.toLowerCase(Locale.ROOT))).orElse(false);
      case FAUX -> brute.map(v -> !"true".equals(v.toLowerCase(Locale.ROOT))).orElse(true);
      case PRESENT -> brute.isPresent();
      case COMMENCE_PAR ->
          brute
              .map(v -> v.toLowerCase(Locale.ROOT).startsWith(valeur.toLowerCase(Locale.ROOT)))
              .orElse(false);
    };
  }

  private static Optional<BigDecimal> nombre(Optional<String> brute) {
    return brute.flatMap(
        v -> {
          try {
            return Optional.of(new BigDecimal(v));
          } catch (NumberFormatException e) {
            return Optional.empty();
          }
        });
  }
}
