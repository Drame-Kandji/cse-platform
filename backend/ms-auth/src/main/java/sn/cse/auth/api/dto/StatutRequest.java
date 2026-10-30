package sn.cse.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Activation / désactivation d'un compte.
 *
 * @param actif statut cible
 */
@Schema(name = "StatutUtilisateur", description = "Statut cible du compte")
public record StatutRequest(
    @Schema(description = "Vrai pour activer, faux pour bloquer la connexion", example = "false")
        @NotNull
        Boolean actif) {}
