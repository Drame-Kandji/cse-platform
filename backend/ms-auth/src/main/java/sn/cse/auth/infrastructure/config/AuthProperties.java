package sn.cse.auth.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import sn.cse.auth.domain.model.Role;

/**
 * Paramètres de ms-auth ({@code cse.auth.*}).
 *
 * @param otpObligatoire rôles pour lesquels la configuration TOTP est exigée (D6)
 * @param synchronisation planification de la synchronisation Keycloak
 * @param portailFournisseur comptes du portail fournisseur créés à la validation d'une inscription
 */
@Validated
@ConfigurationProperties(prefix = "cse.auth")
public record AuthProperties(
    @NotNull List<Role> otpObligatoire,
    @NotNull @Valid Synchronisation synchronisation,
    @NotNull @Valid PortailFournisseur portailFournisseur) {

  /**
   * @param clientId client Keycloak du portail (retour après définition du mot de passe)
   * @param redirectUri URL du portail (doit être autorisée par le client)
   * @param validiteLien durée de validité du lien « définir votre mot de passe »
   * @param prenomContact prénom affiché des comptes créés (le nom est la raison sociale)
   */
  public record PortailFournisseur(
      @NotBlank String clientId,
      @NotBlank String redirectUri,
      @NotNull Duration validiteLien,
      @NotBlank String prenomContact) {}

  /**
   * @param auDemarrage lance une synchronisation dès que l'application est prête
   * @param relance délai avant nouvelle tentative si Keycloak est injoignable au démarrage
   * @param periodique active la synchronisation périodique
   * @param intervalle intervalle de la synchronisation périodique
   */
  public record Synchronisation(
      boolean auDemarrage,
      @NotNull Duration relance,
      boolean periodique,
      @NotNull Duration intervalle) {}
}
