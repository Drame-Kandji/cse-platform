package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/**
 * Ligne de catalogue.
 *
 * @param id identifiant
 * @param reference référence
 * @param designation désignation
 * @param categorie catégorie
 * @param prixUnitaire prix FCFA
 * @param unite unité
 * @param delaiLivraisonJours délai
 * @param dateMaj dernière mise à jour
 */
@Schema(description = "Ligne du catalogue de prix")
public record LigneCatalogueDto(
    @Schema(example = "c1000000-0000-0000-0000-000000000001") UUID id,
    @Schema(example = "PH-CAT320") String reference,
    @Schema(example = "Pelle hydraulique sur chenilles 20 t") String designation,
    @Schema(example = "MATERIEL_ROULANT") CategorieMateriel categorie,
    @Schema(example = "98500000") BigDecimal prixUnitaire,
    @Schema(example = "unité") String unite,
    @Schema(example = "45") int delaiLivraisonJours,
    @Schema(example = "2026-09-01T08:00:00Z") Instant dateMaj) {}
