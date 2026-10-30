package sn.cse.besoins.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.besoins.application.command.ContenuBesoin;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Corps de {@code POST /api/besoins}.
 *
 * @param chantierId chantier demandeur
 * @param designation désignation
 * @param categorie catégorie
 * @param typeEnginId type d'engin (optionnel)
 * @param quantite quantité
 * @param specifications spécifications (optionnel)
 * @param priorite priorité
 * @param dureeUtilisationJours durée d'utilisation
 * @param dateBesoin date de besoin
 * @param justification justification (obligatoire si URGENT/CRITIQUE, R08)
 */
@Schema(name = "SoumissionBesoin", description = "Expression d'un nouveau besoin en matériel")
public record SoumissionBesoinRequest(
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
    @Schema(description = "Quantité demandée", example = "2", minimum = "1")
        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être positive")
        Integer quantite,
    @Schema(
            description = "Spécifications techniques",
            example = "Godet 1 m³, chenilles, conducteur fourni",
            nullable = true)
        @Size(max = 2000, message = "2000 caractères au maximum")
        String specifications,
    @Schema(description = "Priorité", example = "URGENT")
        @NotNull(message = "La priorité est obligatoire")
        Priorite priorite,
    @Schema(description = "Durée d'utilisation prévue (jours)", example = "45", minimum = "1")
        @NotNull(message = "La durée d'utilisation est obligatoire")
        @Positive(message = "La durée doit être positive")
        Integer dureeUtilisationJours,
    @Schema(description = "Date à laquelle le matériel est requis", example = "2026-11-15")
        @NotNull(message = "La date de besoin est obligatoire")
        LocalDate dateBesoin,
    @Schema(
            description = "Justification (obligatoire si URGENT ou CRITIQUE, règle R08)",
            example = "Terrassement du lot 3 bloqué depuis la panne de la pelle principale",
            nullable = true)
        @Size(max = 2000, message = "2000 caractères au maximum")
        String justification) {

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
