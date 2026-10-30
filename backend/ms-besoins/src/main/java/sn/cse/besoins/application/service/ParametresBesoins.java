package sn.cse.besoins.application.service;

/**
 * Paramètres applicatifs issus de la configuration {@code cse.besoins.*}.
 *
 * @param limiteConsolidation nombre maximal de besoins lus pour une consolidation
 */
public record ParametresBesoins(int limiteConsolidation) {

  public ParametresBesoins {
    if (limiteConsolidation < 1) {
      throw new IllegalArgumentException("La limite de consolidation doit être positive");
    }
  }
}
