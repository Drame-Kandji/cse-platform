package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Proposition d'attribution et circuit d'approbation.
 *
 * @param offreId offre proposée
 * @param codeAnonyme code de l'offre
 * @param fournisseurNom fournisseur proposé
 * @param montant montant
 * @param justification justification
 * @param proposeeParNom auteur
 * @param dateProposition instant
 * @param coValidationMateriel co-validation RM
 * @param approbationDg approbation DG (présente si montant &gt; seuil de délégation)
 */
@Schema(name = "Attribution", description = "Proposition d'attribution (R11)")
public record AttributionDto(
    @Schema(example = "6b1f6c1e-2a41-4d63-9a59-1f3e9b7f0c11") UUID offreId,
    @Schema(example = "OFR-7F3A2C") String codeAnonyme,
    @Schema(example = "Sahel Engins SA") String fournisseurNom,
    @Schema(description = "Montant en FCFA", example = "285000000") BigDecimal montant,
    @Schema(example = "Meilleur score global et délai de livraison compatible avec le chantier.")
        String justification,
    @Schema(example = "Fatou Sarr") String proposeeParNom,
    @Schema(example = "2026-10-10T09:00:00Z") Instant dateProposition,
    ApprobationDto coValidationMateriel,
    @Schema(description = "Présente uniquement si le montant dépasse le seuil de délégation")
        ApprobationDto approbationDg) {}
