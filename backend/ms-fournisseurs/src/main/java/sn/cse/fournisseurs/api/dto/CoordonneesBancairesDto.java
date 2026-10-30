package sn.cse.fournisseurs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Coordonnées bancaires (visibles uniquement du Responsable des Achats et de la DAF).
 *
 * @param banque banque
 * @param iban IBAN
 */
@Schema(description = "Coordonnées bancaires (chiffrées au repos ; visibles RA et DAF uniquement)")
public record CoordonneesBancairesDto(
    @Schema(description = "Banque", example = "Banque Atlantique Sénégal")
        @NotBlank
        @Size(max = 150)
        String banque,
    @Schema(description = "IBAN (espaces tolérés)", example = "SN08SN0100152000048500003035")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}[0-9]{2}[A-Za-z0-9 ]{11,40}$", message = "IBAN invalide")
        String iban) {}
