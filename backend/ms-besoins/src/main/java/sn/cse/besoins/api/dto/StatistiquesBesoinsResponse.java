package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

/**
 * Ressource {@code StatistiquesBesoins}.
 *
 * @param total total
 * @param parStatut répartition par statut
 * @param parPriorite répartition par priorité
 * @param delaiMoyenValidationHeures délai moyen de décision en heures
 */
@Schema(name = "StatistiquesBesoins", description = "Indicateurs des besoins visibles")
public record StatistiquesBesoinsResponse(
    @Schema(example = "42") long total,
    @Schema(
            description = "Nombre de besoins par statut (toutes les clés présentes)",
            example =
                "{\"EN_ATTENTE\":9,\"VALIDE\":2,\"REJETE\":6,\"AFFECTE_DIRECT\":14,\"EN_ACHAT\":11}")
        Map<StatutBesoin, Long> parStatut,
    @Schema(
            description = "Nombre de besoins par priorité (toutes les clés présentes)",
            example = "{\"NORMAL\":24,\"URGENT\":12,\"CRITIQUE\":6}")
        Map<Priorite, Long> parPriorite,
    @Schema(
            description = "Délai moyen entre soumission et décision (heures)",
            example = "26.4",
            nullable = true)
        Double delaiMoyenValidationHeures) {

  /**
   * @param s statistiques applicatives
   * @return la ressource
   */
  public static StatistiquesBesoinsResponse depuis(StatistiquesBesoins s) {
    return new StatistiquesBesoinsResponse(
        s.total(), s.parStatut(), s.parPriorite(), s.delaiMoyenValidationHeures());
  }
}
