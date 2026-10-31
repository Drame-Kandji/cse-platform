package sn.cse.audit.domain.model;

import java.util.Comparator;
import java.util.List;

/**
 * Statistiques globales du journal d'audit.
 *
 * @param total nombre total d'entrées
 * @param aujourdhui entrées dont l'action date du jour courant (UTC)
 * @param septDerniersJours entrées des sept derniers jours, jour courant inclus
 * @param parService répartition par service émetteur, triée par volume décroissant
 */
public record StatistiquesAudit(
    long total, long aujourdhui, long septDerniersJours, List<TotalParService> parService) {

  public StatistiquesAudit {
    parService =
        parService == null
            ? List.of()
            : parService.stream()
                .sorted(
                    Comparator.comparingLong(TotalParService::total)
                        .reversed()
                        .thenComparing(TotalParService::service))
                .toList();
  }

  /**
   * Volume d'entrées d'un service.
   *
   * @param service service émetteur
   * @param total nombre d'entrées
   */
  public record TotalParService(String service, long total) {}
}
