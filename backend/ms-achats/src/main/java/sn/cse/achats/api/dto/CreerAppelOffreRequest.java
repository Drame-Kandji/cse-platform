package sn.cse.achats.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.TypeAppelOffre;

/**
 * Publication d'un appel d'offres.
 *
 * @param besoinId besoin d'origine
 * @param objet objet
 * @param description description
 * @param type procédure
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param budgetEstime budget estimé
 * @param dateLimite date limite de dépôt
 * @param criteres critères pondérés (somme 100)
 * @param fournisseurIdsInvites fournisseurs invités (ACTIF, R14)
 */
@Schema(name = "CreerAppelOffreRequest", description = "Publication d'un appel d'offres")
public record CreerAppelOffreRequest(
    @Schema(
            description = "Besoin transmis aux achats (facultatif)",
            example = "b0000000-0000-0000-0000-000000000001")
        UUID besoinId,
    @Schema(example = "Acquisition de deux pelles hydrauliques 20 t") @NotBlank @Size(max = 255)
        String objet,
    @Schema(example = "Pelles sur chenilles pour les terrassements de l'autoroute Dakar–AIBD")
        @Size(max = 4000)
        String description,
    @Schema(example = "APPEL_OFFRES") @NotNull TypeAppelOffre type,
    @Schema(example = "MATERIEL_ROULANT") @NotNull CategorieMateriel categorie,
    @Schema(example = "2") @NotNull @Min(1) Integer quantite,
    @Schema(example = "Godet 1,2 m3, moteur Tier 3") @Size(max = 4000) String specifications,
    @Schema(description = "Budget estimé en FCFA", example = "300000000") @Positive
        BigDecimal budgetEstime,
    @Schema(description = "Date limite de dépôt (instant futur)", example = "2026-11-20T17:00:00Z")
        @NotNull
        Instant dateLimite,
    @Schema(description = "Critères pondérés, somme des poids = 100") @NotEmpty @Valid
        List<CriterePondereDto> criteres,
    @Schema(
            description = "Fournisseurs invités (obligatoire pour une demande de prix)",
            example =
                "[\"f0000000-0000-0000-0000-000000000001\", \"f0000000-0000-0000-0000-000000000002\"]")
        List<@NotNull UUID> fournisseurIdsInvites) {}
