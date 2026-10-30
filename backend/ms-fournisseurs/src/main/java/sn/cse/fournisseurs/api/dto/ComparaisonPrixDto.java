package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/**
 * Comparaison des prix d'un article entre fournisseurs actifs.
 *
 * @param designation désignation
 * @param reference référence commune
 * @param categorie catégorie
 * @param prixMin prix minimal
 * @param prixMax prix maximal
 * @param prixMoyen prix moyen
 * @param offres offres par prix croissant
 */
@Schema(description = "Comparaison de prix multi-fournisseurs")
public record ComparaisonPrixDto(
    @Schema(example = "Groupe électrogène 100 kVA") String designation,
    @Schema(
            description = "Référence commune à toutes les offres, sinon null",
            example = "GE-100KVA")
        String reference,
    @Schema(example = "OUTILLAGE") CategorieMateriel categorie,
    @Schema(example = "14200000") BigDecimal prixMin,
    @Schema(example = "15900000") BigDecimal prixMax,
    @Schema(example = "15050000") BigDecimal prixMoyen,
    @Schema(description = "Offres triées par prix croissant") List<Offre> offres) {

  /**
   * Offre d'un fournisseur.
   *
   * @param fournisseurId fournisseur
   * @param raisonSociale raison sociale
   * @param prixUnitaire prix
   * @param delaiLivraisonJours délai
   * @param scorePerformance score
   */
  @Schema(name = "OffreCatalogue", description = "Offre catalogue d'un fournisseur")
  public record Offre(
      @Schema(example = "f0000000-0000-0000-0000-000000000003") UUID fournisseurId,
      @Schema(example = "Dakar Outillage Pro") String raisonSociale,
      @Schema(example = "14200000") BigDecimal prixUnitaire,
      @Schema(example = "10") int delaiLivraisonJours,
      @Schema(example = "78.4") BigDecimal scorePerformance) {}
}
