package sn.cse.reporting.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Optional;

/** Conventions d'arrondi : montants FCFA entiers, taux et moyennes à une décimale. */
public final class Arrondis {

  private static final BigDecimal CENT = BigDecimal.valueOf(100);

  private Arrondis() {}

  /**
   * @param montant montant (null = 0)
   * @return montant FCFA entier
   */
  public static BigDecimal montant(BigDecimal montant) {
    return (montant == null ? BigDecimal.ZERO : montant).setScale(0, RoundingMode.HALF_UP);
  }

  /**
   * @param numerateur numérateur
   * @param denominateur dénominateur
   * @return pourcentage à une décimale, ou zéro si le dénominateur est nul
   */
  public static BigDecimal taux(long numerateur, long denominateur) {
    return taux(BigDecimal.valueOf(numerateur), BigDecimal.valueOf(denominateur));
  }

  /**
   * @param numerateur numérateur
   * @param denominateur dénominateur
   * @return pourcentage à une décimale, ou zéro si le dénominateur est nul
   */
  public static BigDecimal taux(BigDecimal numerateur, BigDecimal denominateur) {
    if (denominateur.signum() == 0) {
      return BigDecimal.ZERO.setScale(1);
    }
    return numerateur.multiply(CENT).divide(denominateur, 1, RoundingMode.HALF_UP);
  }

  /**
   * @param numerateur numérateur
   * @param denominateur dénominateur
   * @return pourcentage à une décimale, vide si le dénominateur est nul (indicateur non défini)
   */
  public static Optional<BigDecimal> tauxDefini(long numerateur, long denominateur) {
    return denominateur == 0 ? Optional.empty() : Optional.of(taux(numerateur, denominateur));
  }

  /**
   * @param valeurs valeurs
   * @return moyenne à une décimale, vide si aucune valeur
   */
  public static Optional<BigDecimal> moyenne(Collection<BigDecimal> valeurs) {
    if (valeurs.isEmpty()) {
      return Optional.empty();
    }
    BigDecimal somme = valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return Optional.of(somme.divide(BigDecimal.valueOf(valeurs.size()), 1, RoundingMode.HALF_UP));
  }
}
