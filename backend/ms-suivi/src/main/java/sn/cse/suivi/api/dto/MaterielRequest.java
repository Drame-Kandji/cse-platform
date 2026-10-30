package sn.cse.suivi.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Création / modification d'une fiche matériel.
 *
 * @param code code parc unique
 * @param immatriculation immatriculation
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin
 * @param marque marque
 * @param modele modèle
 * @param tarifJournalier coût journalier (FCFA)
 * @param valeurAcquisition valeur d'achat (FCFA)
 * @param dateAcquisition date d'achat
 * @param heuresFonctionnement compteur initial (création uniquement)
 * @param version version lue (obligatoire en modification)
 */
@Schema(name = "MaterielRequest", description = "Caractéristiques d'un matériel")
public record MaterielRequest(
    @Schema(example = "BUL-KOM-03") @NotBlank @Size(max = 40) String code,
    @Schema(example = "DK-8812-BJ", nullable = true) @Size(max = 40) String immatriculation,
    @Schema(example = "Bulldozer sur chenilles 20 t") @NotBlank @Size(max = 200) String designation,
    @Schema(example = "MATERIEL_ROULANT") @NotNull CategorieMateriel categorie,
    @Schema(example = "e0000000-0000-0000-0000-000000000002", nullable = true) UUID typeEnginId,
    @Schema(example = "Komatsu", nullable = true) @Size(max = 100) String marque,
    @Schema(example = "D65EX-18", nullable = true) @Size(max = 100) String modele,
    @Schema(description = "Coût journalier en FCFA", example = "420000") @NotNull @PositiveOrZero
        BigDecimal tarifJournalier,
    @Schema(description = "Valeur d'achat en FCFA", example = "185000000", nullable = true)
        @PositiveOrZero
        BigDecimal valeurAcquisition,
    @Schema(example = "2024-05-20", nullable = true) @PastOrPresent LocalDate dateAcquisition,
    @Schema(
            description = "Compteur horaire initial (ignoré en modification : utiliser le relevé)",
            example = "1250",
            nullable = true)
        @PositiveOrZero
        Long heuresFonctionnement,
    @Schema(
            description = "Version lue (obligatoire en modification)",
            example = "5",
            nullable = true)
        Long version) {}
