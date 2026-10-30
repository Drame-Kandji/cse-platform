package sn.cse.fournisseurs.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import sn.cse.fournisseurs.domain.service.ParametresScore;

/**
 * Paramètres métier du service ({@code cse.fournisseurs.*}), validés au démarrage.
 *
 * @param score politique de score (R07)
 * @param surveillance job de contrôle périodique
 */
@Validated
@ConfigurationProperties(prefix = "cse.fournisseurs")
public record FournisseursProperties(
    @Valid @NotNull Score score, @Valid @NotNull Surveillance surveillance) {

  /**
   * Poids et seuils du score de performance.
   *
   * @param poidsQualite poids du critère qualité
   * @param poidsDelais poids du critère délais
   * @param poidsConformite poids du critère conformité
   * @param poidsSav poids du critère SAV
   * @param poidsPrix poids du critère prix
   * @param poidsLivraisonALHeure part (%) du taux de livraison à l'heure
   * @param penaliteLitigeOuvert points retirés par litige ouvert
   * @param penaliteLitigeResolu points retirés par litige résolu
   * @param scoreInitial score sans historique
   * @param seuilAlerte seuil d'alerte
   */
  public record Score(
      @NotNull @Min(0) Integer poidsQualite,
      @NotNull @Min(0) Integer poidsDelais,
      @NotNull @Min(0) Integer poidsConformite,
      @NotNull @Min(0) Integer poidsSav,
      @NotNull @Min(0) Integer poidsPrix,
      @NotNull @Min(0) @Max(100) Integer poidsLivraisonALHeure,
      @NotNull @DecimalMin("0") BigDecimal penaliteLitigeOuvert,
      @NotNull @DecimalMin("0") BigDecimal penaliteLitigeResolu,
      @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal scoreInitial,
      @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal seuilAlerte) {

    /**
     * @return paramètres du domaine
     */
    public ParametresScore versParametres() {
      return new ParametresScore(
          poidsQualite,
          poidsDelais,
          poidsConformite,
          poidsSav,
          poidsPrix,
          poidsLivraisonALHeure,
          penaliteLitigeOuvert,
          penaliteLitigeResolu,
          scoreInitial,
          seuilAlerte);
    }
  }

  /**
   * Job de surveillance (documents expirés, scores faibles).
   *
   * @param enabled activation du job
   * @param cron expression cron Spring
   * @param zone fuseau horaire du cron
   */
  public record Surveillance(
      @NotNull Boolean enabled, @NotBlank String cron, @NotBlank String zone) {}
}
