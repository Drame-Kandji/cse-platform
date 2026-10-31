package sn.cse.reporting.domain.model;

/**
 * Paramètres métier des calculs (valeurs issues de {@code cse.reporting.*}).
 *
 * @param periodeDefautJours longueur de la période par défaut (90 derniers jours)
 * @param periodeMaxJours longueur maximale d'une période demandée
 * @param maxPointsSerie nombre maximal de points d'une série
 * @param topFournisseurs taille du classement des meilleurs fournisseurs
 * @param dureeChantierDefautJours durée retenue pour proratiser le montant du marché d'un chantier
 *     sans date de fin prévue
 */
public record ParametresReporting(
    int periodeDefautJours,
    int periodeMaxJours,
    int maxPointsSerie,
    int topFournisseurs,
    int dureeChantierDefautJours) {

  public ParametresReporting {
    if (periodeDefautJours < 1
        || periodeMaxJours < periodeDefautJours
        || maxPointsSerie < 1
        || topFournisseurs < 1
        || dureeChantierDefautJours < 1) {
      throw new IllegalArgumentException("Paramètres de reporting incohérents");
    }
  }
}
