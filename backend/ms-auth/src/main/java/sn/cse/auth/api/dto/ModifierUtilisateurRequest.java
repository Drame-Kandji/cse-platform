package sn.cse.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import sn.cse.auth.domain.model.Role;

/**
 * Modification d'un compte (mêmes champs que la création, sans mot de passe).
 *
 * @param email adresse e-mail
 * @param prenom prénom
 * @param nom nom
 * @param telephone téléphone
 * @param role rôle unique
 * @param chantierIds chantiers (chef de chantier)
 * @param perimetreParc périmètre du parc (responsable matériel)
 * @param seuilDelegation seuil de délégation (responsable des achats)
 * @param budgetGere budget géré (DAF)
 * @param fournisseurId fiche fournisseur rattachée (fournisseur ; extension facultative)
 */
@Schema(name = "ModifierUtilisateur", description = "Modification d'un compte")
public record ModifierUtilisateurRequest(
    @Schema(example = "moussa.sy@cse.sn") @NotBlank @Email @Size(max = 255) String email,
    @Schema(example = "Moussa") @NotBlank @Size(max = 150) String prenom,
    @Schema(example = "Sy") @NotBlank @Size(max = 150) String nom,
    @Schema(example = "+221 77 123 45 67", nullable = true)
        @Size(max = 30)
        @Pattern(regexp = "^[+0-9 ().-]*$", message = "numéro de téléphone invalide")
        String telephone,
    @Schema(example = "RESPONSABLE_MATERIEL") @NotNull Role role,
    @Schema(example = "[\"c0000000-0000-0000-0000-000000000001\"]", nullable = true)
        Set<UUID> chantierIds,
    @Schema(example = "NATIONAL", nullable = true) @Size(max = 100) String perimetreParc,
    @Schema(example = "150000000", nullable = true) @PositiveOrZero BigDecimal seuilDelegation,
    @Schema(example = "5000000000", nullable = true) @PositiveOrZero BigDecimal budgetGere,
    @Schema(example = "f0000000-0000-0000-0000-000000000001", nullable = true)
        UUID fournisseurId) {}
