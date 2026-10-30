package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/**
 * Modification du référentiel d'un fournisseur (verrou optimiste).
 *
 * @param raisonSociale raison sociale
 * @param ninea NINEA
 * @param rccm RCCM
 * @param adresse adresse
 * @param ville ville
 * @param pays pays
 * @param telephone téléphone
 * @param email e-mail
 * @param contactNom interlocuteur
 * @param contactFonction fonction
 * @param specialites spécialités
 * @param certifications certifications
 * @param coordonneesBancaires coordonnées bancaires (absentes = inchangées)
 * @param version version connue du client
 */
@Schema(description = "Données de modification d'un fournisseur")
public record ModifierFournisseurRequest(
    @Schema(description = "Raison sociale", example = "Sahel Engins SA") @NotBlank @Size(max = 200)
        String raisonSociale,
    @Schema(description = "NINEA", example = "005123456 2G3")
        @NotBlank
        @Pattern(regexp = "^[0-9]{7,9} ?[0-9A-Za-z]{3}$", message = "NINEA invalide")
        String ninea,
    @Schema(description = "Numéro RCCM", example = "SN-DKR-2015-B-12345") @Size(max = 50)
        String rccm,
    @Schema(description = "Adresse", example = "Km 4,5 boulevard du Centenaire de la Commune")
        @Size(max = 255)
        String adresse,
    @Schema(description = "Ville", example = "Dakar") @Size(max = 100) String ville,
    @Schema(description = "Pays", example = "Sénégal") @NotBlank @Size(max = 100) String pays,
    @Schema(description = "Téléphone", example = "+221 33 849 12 34")
        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$", message = "Numéro de téléphone invalide")
        String telephone,
    @Schema(description = "E-mail de contact", example = "contact@sahel-engins.sn")
        @NotBlank
        @Email
        @Size(max = 200)
        String email,
    @Schema(description = "Interlocuteur", example = "Abdoulaye Mbaye") @Size(max = 150)
        String contactNom,
    @Schema(description = "Fonction de l'interlocuteur", example = "Directeur commercial")
        @Size(max = 150)
        String contactFonction,
    @ArraySchema(
            arraySchema = @Schema(description = "Spécialités"),
            schema =
                @Schema(implementation = CategorieMateriel.class, example = "MATERIEL_ROULANT"))
        @NotEmpty
        Set<CategorieMateriel> specialites,
    @ArraySchema(
            arraySchema = @Schema(description = "Certifications"),
            schema = @Schema(example = "ISO 9001:2015"))
        List<@NotBlank @Size(max = 150) String> certifications,
    @Schema(description = "Coordonnées bancaires (absentes = inchangées)") @Valid
        CoordonneesBancairesDto coordonneesBancaires,
    @Schema(description = "Version connue (verrou optimiste)", example = "3")
        @NotNull
        @PositiveOrZero
        Long version) {}
