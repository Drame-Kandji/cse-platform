package sn.cse.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Réinitialisation du mot de passe.
 *
 * @param motDePasseTemporaire nouveau mot de passe temporaire
 */
@Schema(name = "ReinitialiserMotDePasse", description = "Mot de passe temporaire à imposer")
public record MotDePasseRequest(
    @Schema(
            description = "Mot de passe temporaire, à changer à la prochaine connexion",
            example = "Temp@2026!")
        @NotBlank
        @Size(min = 8, max = 128)
        String motDePasseTemporaire) {

  @Override
  public String toString() {
    return "MotDePasseRequest[***]";
  }
}
