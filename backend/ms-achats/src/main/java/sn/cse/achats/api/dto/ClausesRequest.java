package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Rédaction des clauses (contrat BROUILLON).
 *
 * @param garantieMois garantie
 * @param conditionsMaintenance maintenance
 * @param conditionsLivraison livraison
 * @param penaliteRetardPctParJour pénalité
 * @param delaiLivraisonJours délai
 * @param jalons jalons
 */
@Schema(name = "ClausesRequest", description = "Clauses et jalons d'un contrat BROUILLON")
public record ClausesRequest(
    @Schema(example = "24") @PositiveOrZero Integer garantieMois,
    @Schema(example = "Révision des 500 heures assurée par le fournisseur") @Size(max = 4000)
        String conditionsMaintenance,
    @Schema(example = "Livraison franco sur le chantier de Diamniadio") @Size(max = 4000)
        String conditionsLivraison,
    @Schema(description = "% du montant par jour de retard", example = "0.1")
        @DecimalMin("0")
        @DecimalMax("100")
        BigDecimal penaliteRetardPctParJour,
    @Schema(example = "45") @NotNull @Min(1) Integer delaiLivraisonJours,
    @Valid List<JalonRequest> jalons) {

  /**
   * Jalon à planifier.
   *
   * @param libelle libellé
   * @param datePrevue échéance
   */
  @Schema(name = "JalonRequest", description = "Jalon à planifier")
  public record JalonRequest(
      @Schema(example = "Expédition depuis le port de Dakar") @NotBlank @Size(max = 200)
          String libelle,
      @Schema(example = "2026-11-10") @NotNull LocalDate datePrevue) {}
}
