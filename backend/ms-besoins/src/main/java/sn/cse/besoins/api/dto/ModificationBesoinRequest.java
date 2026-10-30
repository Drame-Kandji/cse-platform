package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.besoins.application.command.ContenuBesoin;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Corps de {@code PUT /api/besoins/{id}} : mêmes champs que la soumission + version.
 *
 * @param chantierId chantier
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin
 * @param quantite quantité
 * @param specifications spécifications
 * @param priorite priorité
 * @param dureeUtilisationJours durée
 * @param dateBesoin date de besoin
 * @param justification justification
 * @param version version connue (verrou optimiste)
 */
@Schema(name = "ModificationBesoin", description = "Modification d'un besoin en attente (R05)")
public record ModificationBesoinRequest(
    @Schema(description = "Chantier demandeur", example = "c0000000-0000-0000-0000-000000000001")
        @NotNull(message = "Le chantier est obligatoire")
        UUID chantierId,
    @Schema(description = "Désignation du matériel", example = "Pelle hydraulique 20 t")
        @NotBlank(message = "La désignation est obligatoire")
        @Size(max = 200, message = "200 caractères au maximum")
        String designation,
    @Schema(description = "Catégorie de matériel", example = "MATERIEL_ROULANT")
        @NotNull(message = "La catégorie est obligatoire")
        CategorieMateriel categorie,
    @Schema(
            description = "Type d'engin du référentiel (optionnel)",
            example = "e0000000-0000-0000-0000-000000000001",
            nullable = true)
        UUID typeEnginId,
    @Schema(description = "Quantité demandée", example = "3", minimum = "1")
        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être positive")
        Integer quantite,
    @Schema(
            description = "Spécifications techniques",
            example = "Godet 1 m³, chenilles",
            nullable = true)
        @Size(max = 2000, message = "2000 caractères au maximum")
        String specifications,
    @Schema(description = "Priorité", example = "NORMAL")
        @NotNull(message = "La priorité est obligatoire")
        Priorite priorite,
    @Schema(description = "Durée d'utilisation prévue (jours)", example = "30", minimum = "1")
        @NotNull(message = "La durée d'utilisation est obligatoire")
        @Positive(message = "La durée doit être positive")
        Integer dureeUtilisationJours,
    @Schema(description = "Date à laquelle le matériel est requis", example = "2026-11-20")
        @NotNull(message = "La date de besoin est obligatoire")
        LocalDate dateBesoin,
    @Schema(
            description = "Justification (obligatoire si URGENT ou CRITIQUE, règle R08)",
            example = "Accélération du planning imposée par le maître d'ouvrage",
            nullable = true)
        @Size(max = 2000, message = "2000 caractères au maximum")
        String justification,
    @Schema(description = "Version connue du besoin (verrou optimiste)", example = "0")
        @NotNull(message = "La version est obligatoire")
        @PositiveOrZero(message = "Version invalide")
        Long version) {

  /**
   * @return le contenu applicatif
   */
  public ContenuBesoin contenu() {
    return new ContenuBesoin(
        chantierId,
        designation,
        categorie,
        typeEnginId,
        quantite,
        specifications,
        priorite,
        dureeUtilisationJours,
        dateBesoin,
        justification);
  }
}
