package sn.cse.suivi.application.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Paramètres métier du service ({@code cse.suivi.*}), validés au démarrage. Aucune valeur n'est
 * codée en dur : les valeurs par défaut sont dans {@code application.yml}.
 *
 * @param alertes alertes planifiées
 * @param historique profondeur des historiques de la fiche matériel
 * @param disponibilite recherche de disponibilité
 * @param planning planning des affectations
 * @param livraison intégration des livraisons de marchés
 * @param numerotation numérotation des références
 */
@Validated
@ConfigurationProperties(prefix = "cse.suivi")
public record SuiviParametres(
    @Valid @NotNull Alertes alertes,
    @Valid @NotNull Historique historique,
    @Valid @NotNull Disponibilite disponibilite,
    @Valid @NotNull Planning planning,
    @Valid @NotNull Livraison livraison,
    @Valid @NotNull Numerotation numerotation) {

  /**
   * @param actives active les contrôles planifiés sur cette instance
   * @param maintenanceCron cron du contrôle de maintenance préventive
   * @param finAffectationCron cron du contrôle des fins d'affectation
   * @param finAffectationJoursAvant fenêtre d'alerte avant la fin prévue (J-n)
   * @param rappelMaintenanceJours délai minimal entre deux alertes de maintenance d'un matériel
   */
  public record Alertes(
      boolean actives,
      @NotBlank String maintenanceCron,
      @NotBlank String finAffectationCron,
      @Min(0) @Max(60) int finAffectationJoursAvant,
      @Min(1) @Max(365) int rappelMaintenanceJours) {}

  /**
   * @param taille nombre d'éléments de chaque historique (affectations, maintenances…)
   */
  public record Historique(@Min(1) @Max(100) int taille) {}

  /**
   * @param maxResultats nombre maximal de matériels proposés
   */
  public record Disponibilite(@Min(1) @Max(200) int maxResultats) {}

  /**
   * @param maxJours amplitude maximale d'une requête de planning
   * @param joursAvantParDefaut début par défaut (aujourd'hui moins n jours)
   * @param joursApresParDefaut fin par défaut (aujourd'hui plus n jours)
   */
  public record Planning(
      @Min(1) @Max(3660) int maxJours,
      @Min(0) int joursAvantParDefaut,
      @Min(1) int joursApresParDefaut) {}

  /**
   * @param dureeAffectationJours durée de l'affectation créée à la livraison
   * @param ratioTarifJournalier part de la valeur unitaire retenue comme tarif journalier quand le
   *     contrat n'en fournit pas
   */
  public record Livraison(
      @Min(1) int dureeAffectationJours,
      @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal ratioTarifJournalier) {}

  /**
   * @param prefixeOrdreTravail préfixe des numéros d'ordre de travail (ex. {@code OT})
   * @param prefixesCodeMateriel préfixe des codes générés par catégorie
   */
  public record Numerotation(
      @NotBlank String prefixeOrdreTravail,
      @NotEmpty Map<CategorieMateriel, String> prefixesCodeMateriel) {}
}
