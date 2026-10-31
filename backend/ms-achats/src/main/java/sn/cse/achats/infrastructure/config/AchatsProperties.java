package sn.cse.achats.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Paramètres métier et techniques de ms-achats ({@code cse.achats.*}), validés au démarrage.
 *
 * @param seuilDelegation montant (FCFA) au-delà duquel l'approbation DG est requise (R11)
 * @param penaliteRetardPctParJourDefaut pénalité de retard par défaut (% du montant par jour)
 * @param plafondPenalitesPct plafond des pénalités (% du montant)
 * @param tauxTarifJournalier tarif journalier estimé d'un matériel livré (fraction du prix
 *     unitaire)
 * @param intervalleRappelRetardJours intervalle minimal entre deux alertes de retard
 * @param fournisseurs accès à ms-fournisseurs
 * @param jobs planification des traitements automatiques
 */
@Validated
@ConfigurationProperties(prefix = "cse.achats")
public record AchatsProperties(
    @NotNull @Positive BigDecimal seuilDelegation,
    @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal penaliteRetardPctParJourDefaut,
    @NotNull @Positive @DecimalMax("100") BigDecimal plafondPenalitesPct,
    @NotNull @Positive BigDecimal tauxTarifJournalier,
    @Min(1) int intervalleRappelRetardJours,
    @NotNull @Valid Fournisseurs fournisseurs,
    @NotNull @Valid Jobs jobs) {

  /**
   * @param baseUrl URL de base de ms-fournisseurs
   */
  public record Fournisseurs(@NotBlank String baseUrl) {}

  /**
   * @param clotureCron cron de clôture automatique des appels d'offres échus ({@code -} pour
   *     désactiver)
   * @param retardLivraisonCron cron du contrôle des retards de livraison ({@code -} pour
   *     désactiver)
   */
  public record Jobs(@NotBlank String clotureCron, @NotBlank String retardLivraisonCron) {}
}
