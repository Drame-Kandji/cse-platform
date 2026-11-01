package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/**
 * Auto-inscription publique d'un fournisseur (contrat §13, {@code Inscription}).
 *
 * @param raisonSociale raison sociale
 * @param ninea NINEA
 * @param rccm RCCM
 * @param adresse adresse
 * @param ville ville
 * @param pays pays
 * @param telephone téléphone
 * @param email e-mail de contact (identifiant du futur compte du portail)
 * @param contactNom interlocuteur
 * @param contactFonction fonction
 * @param specialites spécialités
 */
@Schema(description = "Demande d'inscription d'un fournisseur sur le portail")
public record InscriptionRequest(
    @Schema(description = "Raison sociale", example = "Baobab Logistique SARL")
        @NotBlank
        @Size(max = 200)
        String raisonSociale,
    @Schema(description = "NINEA (7 à 9 chiffres + code 3 caractères)", example = "008789012 2E7")
        @NotBlank
        @Pattern(regexp = "^[0-9]{7,9} ?[0-9A-Za-z]{3}$", message = "NINEA invalide")
        String ninea,
    @Schema(description = "Numéro RCCM", example = "SN-THS-2019-B-04567") @Size(max = 50)
        String rccm,
    @Schema(description = "Adresse", example = "Route de Mbour, zone industrielle") @Size(max = 255)
        String adresse,
    @Schema(description = "Ville", example = "Thiès") @Size(max = 100) String ville,
    @Schema(description = "Pays", example = "Sénégal") @NotBlank @Size(max = 100) String pays,
    @Schema(description = "Téléphone", example = "+221 33 951 22 10")
        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$", message = "Numéro de téléphone invalide")
        String telephone,
    @Schema(
            description =
                "E-mail de contact : il deviendra l'identifiant de connexion au portail et recevra"
                    + " le lien de définition du mot de passe après validation",
            example = "contact@baobab-logistique.sn")
        @NotBlank
        @Email
        @Size(max = 200)
        String email,
    @Schema(description = "Interlocuteur", example = "Seynabou Gueye") @NotBlank @Size(max = 150)
        String contactNom,
    @Schema(description = "Fonction de l'interlocuteur", example = "Gérante") @Size(max = 150)
        String contactFonction,
    @ArraySchema(
            arraySchema = @Schema(description = "Spécialités (au moins une catégorie de matériel)"),
            schema = @Schema(implementation = CategorieMateriel.class, example = "CONSOMMABLE"))
        @NotEmpty
        Set<CategorieMateriel> specialites) {}
