package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Clauses particulières.
 *
 * @param garantieMois garantie
 * @param conditionsMaintenance maintenance
 * @param conditionsLivraison livraison
 * @param penaliteRetardPctParJour pénalité (% du montant par jour)
 */
@Schema(name = "ClausesContrat", description = "Clauses particulières du contrat")
public record ClausesDto(
    @Schema(example = "24") Integer garantieMois,
    @Schema(example = "Révision des 500 heures assurée par le fournisseur")
        String conditionsMaintenance,
    @Schema(example = "Livraison franco sur le chantier de Diamniadio") String conditionsLivraison,
    @Schema(example = "0.1") BigDecimal penaliteRetardPctParJour) {}
