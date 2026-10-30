package sn.cse.besoins.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Paramètres métier et techniques du service ({@code cse.besoins.*}), validés au démarrage.
 *
 * @param reference format des références
 * @param consolidation paramètres de consolidation
 * @param validation chaîne de validation configurable (ADR-0010)
 * @param suivi accès à ms-suivi
 */
@Validated
@ConfigurationProperties(prefix = "cse.besoins")
public record BesoinsProperties(
    @Valid @NotNull Reference reference,
    @Valid @NotNull Consolidation consolidation,
    @Valid @NotNull Validation validation,
    @Valid @NotNull Suivi suivi) {

  /**
   * @param prefixe préfixe des références (ex. {@code BES})
   * @param chiffres nombre de chiffres du numéro séquentiel
   */
  public record Reference(@NotBlank String prefixe, @Min(1) int chiffres) {}

  /**
   * @param fenetreDoublonJours écart maximal (jours) entre deux dates de besoin d'un même chantier
   *     pour signaler un doublon potentiel
   * @param limite nombre maximal de besoins lus par consolidation
   */
  public record Consolidation(@Min(0) int fenetreDoublonJours, @Min(1) int limite) {}

  /**
   * @param etapes étapes dans l'ordre de franchissement ; seules les étapes actives sont chaînées
   */
  public record Validation(@NotEmpty List<@Valid Etape> etapes) {}

  /**
   * Étape de validation configurable.
   *
   * @param code code stable de l'étape
   * @param role rôle Keycloak habilité
   * @param actif vrai si l'étape est chaînée
   * @param quantiteMinimale seuil de quantité déclenchant l'étape (optionnel)
   * @param prioriteMinimale seuil de priorité déclenchant l'étape (optionnel)
   */
  public record Etape(
      @NotBlank String code,
      @NotBlank String role,
      boolean actif,
      @Min(1) Integer quantiteMinimale,
      Priorite prioriteMinimale) {}

  /**
   * @param baseUrl URL de base de ms-suivi
   */
  public record Suivi(@NotBlank String baseUrl) {}
}
