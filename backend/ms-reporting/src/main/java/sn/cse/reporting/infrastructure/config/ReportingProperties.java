package sn.cse.reporting.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Paramètres de ms-reporting ({@code cse.reporting.*}).
 *
 * @param periodeDefautJours période par défaut d'un tableau de bord (90 derniers jours)
 * @param periodeMaxJours longueur maximale d'une période demandée
 * @param maxPointsSerie nombre maximal de points d'une série
 * @param topFournisseurs taille du classement des fournisseurs
 * @param dureeChantierDefautJours durée de proratisation du marché d'un chantier sans date de fin
 * @param consommateur préfixe du nom de consommateur Kafka pour l'idempotence
 * @param instantaneCron expression cron (UTC) de l'instantané quotidien du parc ({@code -} pour
 *     désactiver)
 */
@Validated
@ConfigurationProperties(prefix = "cse.reporting")
public record ReportingProperties(
    @NotNull @Min(1) @Max(3660) Integer periodeDefautJours,
    @NotNull @Min(1) @Max(3660) Integer periodeMaxJours,
    @NotNull @Min(1) @Max(5000) Integer maxPointsSerie,
    @NotNull @Min(1) @Max(100) Integer topFournisseurs,
    @NotNull @Min(1) @Max(3660) Integer dureeChantierDefautJours,
    @NotBlank String consommateur,
    @NotBlank String instantaneCron) {}
